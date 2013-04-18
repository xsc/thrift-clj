(ns ^{ :doc "Thrift Server Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.server.core
  (:require [thrift-clj.gen.services :as s]
            [thrift-clj.protocol.core :as proto])
  (:import [org.apache.thrift.server
             TServer TServer$Args
             TSimpleServer
             TThreadPoolServer TThreadPoolServer$Args
             TNonblockingServer TNonblockingServer$Args]
           [org.apache.thrift.transport
             TServerSocket
             TServerTransport
             TNonblockingServerTransport
             TNonblockingServerSocket]
           [org.apache.thrift TProcessor]
           [java.net InetSocketAddress]))

;; ## Helpers

(defn- ^TServerTransport create-blocking-transport
  [port {:keys[bind client-timeout]}]
  (if bind
    (let [^InetSocketAddress addr (InetSocketAddress. (str bind) (int port))]
      (TServerSocket. addr (int (or client-timeout 0))))
    (TServerSocket. (int port) (int (or client-timeout 0)))))

(defn- ^TNonblockingServerTransport create-nonblocking-transport
  [port {:keys[bind client-timeout]}]
  (if bind
    (let [^InetSocketAddress addr (InetSocketAddress. (str bind) (int port))]
      (TNonblockingServerSocket. addr (int (or client-timeout 0))))
    (TNonblockingServerSocket. (int port) (int (or client-timeout 0)))))

(defmacro ^:private wrap-args
  [cls transport iface opts]
  `(let [opts# (apply hash-map ~opts)
         p# (:protocol opts# :compact)
         proto# (apply proto/protocol-factory (if (keyword? p#) [p#] p#))]
     (doto (new ~cls ~transport)
       (.protocolFactory proto#)
       (.processor (s/iface->processor ~iface)))))

;; ## Server Types

(defn ^TServer single-threaded-server
  "Create single-threaded Server using the given Iface Implementation."
  [iface port & opts]
  (let [t (create-blocking-transport port opts)]
    (TSimpleServer. (wrap-args TServer$Args t iface opts))))

(defn ^TServer multi-threaded-server
  "Create multi-threaded Server using the given Iface Implementation."
  [iface port & opts]
  (let [t (create-blocking-transport port opts)]
    (TThreadPoolServer. (wrap-args TThreadPoolServer$Args t iface opts))))

(defn ^TServer nonblocking-server
  "Create non-blocking Server using the given Iface Implementation."
  [iface port & opts]
  (let [t (create-nonblocking-transport port opts)]
    (TNonblockingServer. (wrap-args TNonblockingServer$Args t iface opts))))

;; ## Start/Stop

(defn serve-and-block!
  "Start Server, blocking the current Thread indefinitely."
  [^TServer server]
  (.serve server))

(defn serve!
  "Start Server in a new Thread."
  [^TServer server]
  (future (serve-and-block! server)))

(defn stop!
  "Stop the given Server."
  [^TServer server]
  (.stop server)
  server)
