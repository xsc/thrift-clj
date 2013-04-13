(ns ^{ :doc "Wrappers for Server Creation"
       :author "Yannick Scherer" }
  thrift-clj.server
  (:require [thrift-clj.gen.services :as s])
  (:import (org.apache.thrift.server
             TServer TServer$Args
             TSimpleServer
             TThreadPoolServer TThreadPoolServer$Args
             TNonblockingServer TNonblockingServer$Args)
           (org.apache.thrift.transport
             TServerSocket
             TServerTransport
             TNonblockingServerSocket)
           (org.apache.thrift TProcessor)))

;; ## Helpers

(defmacro ^:private wrap-args
  [cls transport processor]
  `(-> (new ~cls ~transport)
     (.processor (s/iface->processor ~processor))))

;; ## Server Transport

(defmulti ^TServerTransport create-server-transport
  "Create a Server of the given type using the given Processor."
  (fn [k & args] k))

;; ### :socket

(defmethod create-server-transport :socket
  [_ port]
  (TServerSocket. port))

;; ### :socket-nio

(defmethod create-server-transport :socket-nio
  [_ port]
  (TNonblockingServerSocket. port))

;; ## Server

(defn ^TServer single-threaded-server
  "Create single-threaded Server using the given Processor."
  [^TProcessor proc k & args]
  (let [trans (apply create-server-transport k args)]
    (TSimpleServer.
      (wrap-args TServer$Args trans proc))))

(defn ^TServer multi-threaded-server
  "Create multi-threaded Server using the given Processor."
  [^TProcessor proc k & args]
  (let [trans (apply create-server-transport k args)]
    (TThreadPoolServer.
      (wrap-args TThreadPoolServer$Args trans proc))))

(defn ^TServer nonblocking-server
  "Create non-blocking Server using the given Processor."
  [^TProcessor proc k & args]
  (let [trans (apply create-server-transport k args)]
    (TNonblockingServer.
      (wrap-args TNonblockingServer$Args trans proc))))

;; ## Run/Stop

(defn start-server!
  "Start the given Server."
  [^TServer srv]
  (.serve srv)
  srv)

(defn stop-server!
  "Stop the given Server."
  [^TServer srv]
  (.stop srv)
  srv)
