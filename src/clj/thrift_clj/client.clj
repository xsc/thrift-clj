(ns ^{ :doc "Wrappers for Client Creation."
       :author "Yannick Scherer" }
  thrift-clj.client
  (:import (org.apache.thrift.transport
             TTransport
             TSocket)
           (org.apache.thrift.protocol
             TBinaryProtocol)))

;; ## Base

(defmulti ^TTransport create-client-transport
  "Multimethod to create Client Transport."
  (fn [k & args] k))

;; ### :socket

(defmethod create-client-transport :socket
  [_ host port]
  (TSocket. host port))
