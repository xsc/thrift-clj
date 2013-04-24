(ns ^{ :doc "Thrift Server Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.server
  (:require [thrift-clj.gen.services :as s]
            [thrift-clj.transports :as t]
            [thrift-clj.protocol.core :as proto])
  (:import [org.apache.thrift.server
            TServer TSimpleServer 
            TThreadPoolServer TNonblockingServer 
            TServer$Args TServer$AbstractServerArgs 
            TThreadPoolServer$Args TNonblockingServer$Args]
           [org.apache.thrift.transport TServerSocket TNonblockingServerSocket] ;; why do I need these?
           [org.apache.thrift TProcessor]))

;; ## Helpers

(defn- wrap-args
  ^TServer$AbstractServerArgs
  [^TServer$AbstractServerArgs base iface opts]
  (let [opts (apply hash-map opts)
        p (:protocol opts :compact)
        proto (if (keyword? p)
                (proto/protocol-factory p)
                (apply proto/protocol-factory p))]
    (doto base
      (.protocolFactory proto)
      (.processor (s/iface->processor iface)))))

;; ## Server Types

(defn single-threaded-server
  "Create single-threaded Server using the given Iface Implementation."
  ^TServer
  [iface port & opts]
  (let [t (t/blocking-server-transport port opts)]
    (TSimpleServer. (wrap-args (TServer$Args. t) iface opts))))

(defn multi-threaded-server
  "Create multi-threaded Server using the given Iface Implementation."
  ^TServer
  [iface port & opts]
  (let [t (t/blocking-server-transport port opts)]
    (TThreadPoolServer. (wrap-args (TThreadPoolServer$Args. t) iface opts))))

(defn nonblocking-server
  "Create non-blocking Server using the given Iface Implementation."
  ^TServer
  [iface port & opts]
  (let [t (t/nonblocking-server-transport port opts)]
    (TNonblockingServer. (wrap-args (TNonblockingServer$Args. t) iface opts))))

;; ## Start/Stop

(defn serve-and-block!
  "Start Server, blocking the current Thread indefinitely."
  [^TServer server]
  (.serve server))

(defn serve!
  "Start Server in a new Thread. Returns the Server."
  [^TServer server]
  (future (serve-and-block! server))
  server)

(defn stop!
  "Stop the given Server. Returns the Server."
  [^TServer server]
  (.stop server)
  server)
