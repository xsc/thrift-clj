(ns ^{ :doc "Clojure Client Wrapper"
       :author "Yannick Scherer" }
  thrift-clj.core.thrift-clients
  (:require [thrift-clj.reflect :as reflect]
            [thrift-clj.thrift :as thrift]
            [thrift-clj.utils :as u]
            [thrift-clj.client :as c])
  (:use thrift-clj.core.thrift-types))

;; ## Protocol

(defprotocol Client
  "Protocol for Clients."
  (start-client! [this])
  (stop-client! [this]))

(defmacro with-client
  "Start Client, execute Body, stop Client."
  [client & body]
  `(let [c# ~client]
     (try
       (do
         (start-client! c#)
         ~@body)
       (catch Exception e#
         (throw e#))
       (finally
         (stop-client! c#)))))

;; ## Multimethod

(defmulti wrap-client
  "Wrap Thrift Client to satisfy protocol Client."
  (fn [cls client transport] cls))

(defmacro create-client
  "Create Client."
  [cls k & args]
  `(let [t# (c/create-client-transport ~k ~@args)
         p# (org.apache.thrift.protocol.TBinaryProtocol. t#)]
     (wrap-client ~cls (new ~(u/inner cls "Client") p#) t#)))

;; ## Form Generation

(defn generate-thrift-client
  "Make a Client accessible via `create-client`."
  [n cls mth]
  (let [iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        client (gensym "client-")
        wrap-sym (gensym (str n "Client"))]
    `(do
       (deftype ~wrap-sym [~client transport#]
         Client
         (start-client! [~'_]
           (.open transport#))
         (stop-client! [~'_]
           (.close transport#))

         ~iface
         ~@(for [{:keys[name params]} mth]
             (let [params (take (count params) param-syms)]
               `(~(symbol name) [this# ~@params]
                   (. ~client ~(symbol name) ~@params)))))
       ~@(for [{:keys[name params]} mth]
           (let [params (take (count params) param-syms)]
             `(defn ~(symbol (str n "->" name))
                [client# ~@params]
                (thrift->clj
                  (. client# ~(symbol name) 
                     ~@(map #(list `clj->thrift %) params))))))
       (defmethod wrap-client ~cls
         [~'_ client# transport#]
         (new ~wrap-sym client# transport#)))))
