(ns ^{ :doc "Client Infrastructure"
       :author "Yannick Scherer" }
  thrift-clj.client.core
  (:require [thrift-clj.protocol.core :as proto]
            [thrift-clj.client.transport :as t])
  (:import [org.apache.thrift.transport TTransport]
           [org.apache.thrift.protocol TProtocol]))

;; ## Client Instance
;;
;; This multimethod should be implemented to create new instances of Service
;; Clients. Clients should implement `java.io.Closeable` to be usable with
;; `with-open`.

(defmulti connect!*
  "Create new Client of the given Class using the given options (e.g. Transport,
   Protocol, ...)."
  (fn [class ^TProtocol protocol ^TTransport transport] class)
  :default nil)

(defmethod connect!* nil
  [class & _]
  (throw (Exception. (str "Could not create Client: " class))))

(defn connect!
  "Create new Client of the given Class that connects to the given
   service."
  [client-class transport & {:keys[protocol]}]
  (let [[proto-id & proto-args] (let [protocol (or protocol :compact)]
                                  (if (keyword? protocol)
                                    [protocol]
                                    protocol))
        ^TTransport trans (t/to-transport transport)
        proto (apply proto/protocol proto-id trans proto-args)]
    (connect!* client-class proto trans)))

(defn disconnect!
  "Close Client Transport."
  [^java.io.Closeable client]
  (.close client))
