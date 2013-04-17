(ns ^{ :doc "Clojure Client Wrapper"
       :author "Yannick Scherer" }
  thrift-clj.gen.clients
  (:require [thrift-clj.gen.iface :as ifc]
            [thrift-clj.client.core :as client]
            [thrift-clj.thrift.services :as s :only [thrift-service-methods]]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp]))

;; ## Clients
;;
;; Clients need an alias that will be used as a var containing a reference to
;; the client class. Also, the service interface will be imported using that alias.

;; ## Multimethods

(defmulti wrap-client
  "Wrap Thrift Client to satisfy protocol Client."
  (fn [cls client transport] cls))

(defmulti new-client
  "Create new Instance of a Client of the given Class, using the
   given Protocol."
  (fn [cls protocol] cls))

;; ## Form Generation Helpers

(defn- generate-client-type
  "Generate Type that delegates to a contained Client, implementing `java.io.Closeable` 
   for use with `with-open`."
  [client-sym cls mth]
  (let [iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        c (gensym)]
    `(deftype ~client-sym [~c transport#]
       java.io.Closeable
       (close [this#] (.close transport#))
       ~iface
       ~@(for [{:keys[name params]} mth]
           (let [params (take (count params) param-syms)]
             `(~(symbol name) [~'_ ~@params]
                 (. ~c ~(symbol name) ~@params)))))))

(defn- generate-client-defmethods
  "Generate Implementation of `thrift-clj.client.core/connect!*`"
  [client-sym cls]
  (let [cln (u/inner cls "Client")]
    `(do
       (defmethod client/connect!* ~cln
         [~'_ protocol# transport#]
         (let [client# (new ~client-sym (new ~cln protocol#) transport#)]
           (.open transport#)
           client#)))))

;; ## Import

(nsp/def-reload-indicator reload-client?)

(defn generate-thrift-client-imports
  "Import Thrift Clients for map of service-class/client-name pairs."
  [service-map]
  (for [[service-class client-alias] service-map]
    (let [cls (u/full-class-symbol service-class)
          cln (u/inner cls "Client")]
      (try
        (let [client-alias (or client-alias (u/class-symbol service-class))
              mth (s/thrift-service-methods service-class)
              client-sym (gensym)]
          `(do
             ~@(when (reload-client? cln)
                 [`(nsp/internal-ns-remove '~cln)])
             (nsp/internal-ns 
               ~cln
               ~(generate-client-type client-sym cls mth)
               ~(generate-client-defmethods client-sym cls))
             ~(ifc/generate-thrift-iface-import service-class client-alias)
             (def ~client-alias ~cln)
             true))
        (catch Exception ex
          (throw (Exception. (str "Failed to import Client: " cln) ex)))))))
