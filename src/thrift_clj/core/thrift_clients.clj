(ns ^{ :doc "Clojure Client Wrapper"
       :author "Yannick Scherer" }
  thrift-clj.core.thrift-clients
  (:require [thrift-clj.reflect :as reflect]
            [thrift-clj.thrift :as thrift]
            [thrift-clj.utils :as u])
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

;; ## Multimethods

(defmulti wrap-client
  "Wrap Thrift Client to satisfy protocol Client."
  (fn [cls client transport] cls))

(defmulti new-client
  (fn [cls protocol] cls))

;; ## Form Generation

(defn generate-thrift-client
  "Make a Client accessible via `create-client`."
  [n cls mth]
  (let [iface (u/inner cls "Iface")
        cln (u/inner cls "Client")
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
                (->clj
                  (. client# ~(symbol name) 
                     ~@(map #(list `->thrift %) params))))))
       (defmethod wrap-client ~cln
         [~'_ client# transport#]
         (new ~wrap-sym client# transport#))
       (defmethod new-client ~cln
         [~'_ proto#]
         (new ~cln proto#))
       (def ~n ~cln))))

(defn import-thrift-clients
  "Import Thrift Clients for map of service-class/client-name pairs."
  [service-map]
  (for [[s n] service-map]
    (let [n (or n (u/class-symbol s))
          cls (u/full-class-symbol s)
          mth (reflect/thrift-service-methods s)]
      (generate-thrift-client n cls mth))))
