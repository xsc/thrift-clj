(ns ^{ :doc "Thrift Server Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.server
  (:require [thrift-clj.gen.services :as s]
            [thrift-clj.transports :as t]
            [thrift-clj.protocol.core :as proto])
  (:import [org.apache.thrift.server
            TServer
            TSimpleServer
            TThreadPoolServer
            TNonblockingServer]
           [org.apache.thrift.transport TServerSocket TNonblockingServerSocket] ;; why do I need these?
           [org.apache.thrift TProcessor]))

;; ## Helpers

(defn- opts->protocol-factory
  [{:keys [protocol] :or {protocol :compact}}]
  (if (keyword? protocol)
    (proto/protocol-factory protocol)
    (apply proto/protocol-factory protocol)))

(defmacro ^:private construct-server
  "Call server constructor, depending on Thrift version."
  [args-class-name class-name transport iface opts & [transport-factory]]
  (let [args-n (str "org.apache.thrift.server." args-class-name)
        class-n (str "org.apache.thrift.server." class-name)]
    (if (try
          (Class/forName args-n)
          (catch ClassNotFoundException _))
      `(new
         ~(symbol class-n)
         (doto (new ~(symbol args-n) ~transport)
           (.protocolFactory (opts->protocol-factory ~opts))
           (.processor (s/iface->processor ~iface))))
      `(new
         ~(symbol class-n)
         (s/iface->processor ~iface)
         ~transport
         ~(or transport-factory
              `(org.apache.thrift.transport.TTransportFactory.))
         (opts->protocol-factory ~opts)))))

;; ## Server Types

(defn single-threaded-server
  "Create single-threaded Server using the given Iface Implementation."
  ^TServer
  [iface port & {:as opts}]
  (construct-server
    TServer$Args
    TSimpleServer
    (t/blocking-server-transport port opts)
    iface
    opts))

(defn multi-threaded-server
  "Create multi-threaded Server using the given Iface Implementation."
  ^TServer
  [iface port & {:as opts}]
  (construct-server
    TThreadPoolServer$Args
    TThreadPoolServer
    (t/blocking-server-transport port opts)
    iface
    opts))

(defn nonblocking-server
  "Create non-blocking Server using the given Iface Implementation."
  ^TServer
  [iface port & {:as opts}]
  (construct-server
    TNonblockingServer$Args
    TNonblockingServer
    (t/nonblocking-server-transport port opts)
    iface
    opts
    (org.apache.thrift.transport.TFramedTransport$Factory.)))

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
