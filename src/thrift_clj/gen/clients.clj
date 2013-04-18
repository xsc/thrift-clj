(ns ^{ :doc "Clojure Client Wrapper"
       :author "Yannick Scherer" }
  thrift-clj.gen.clients
  (:require [thrift-clj.gen.iface :as ifc]
            [thrift-clj.client.core :as client]
            [thrift-clj.thrift.services :as s :only [thrift-service-methods]]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp])
  (:import org.apache.thrift.transport.TTransport
           org.apache.thrift.protocol.TProtocol))


;; ## Clients
;;
;; Clients need an alias that will be used as a var containing a reference to
;; the client class. Also, the service interface will be imported using that alias.

;; ## Form Generation Helpers

(defn- generate-client-type
  "Generate Type that delegates to a contained Client, implementing `java.io.Closeable` 
   for use with `with-open`."
  [client-sym cls mth]
  (let [iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        c (vary-meta (gensym) assoc :tag iface)
        t (vary-meta (gensym "transport-") assoc :tag `TTransport)]
    `(deftype ~client-sym [~c ~t]
       java.io.Closeable
       (close [this#] (.close ~t))
       ~iface
       ~@(for [{:keys[name params]} mth]
           (let [params (take (count params) param-syms)]
             `(~(symbol name) [~'_ ~@params]
                 (. ~c ~(symbol name) ~@params)))))))

(defn- generate-client-defmethods
  "Generate Implementation of `thrift-clj.client.core/connect!*`"
  [client-sym cls]
  (let [cln (u/inner cls "Client")
        p (vary-meta (gensym "protocol-") assoc :tag `TProtocol)
        t (vary-meta (gensym "transport-") assoc :tag `TTransport)]
    `(do
       (defmethod client/connect!* ~cln
         [~'_ ~p ~t]
         (let [client# (new ~client-sym (new ~cln ~p) ~t)]
           (.open ~t)
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
