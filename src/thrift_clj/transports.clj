(ns ^{ :doc "Thrift Transports"
       :author "Yannick Scherer" }
  thrift-clj.transports
  (:import [org.apache.thrift.transport
            TTransport TSocket TNonblockingSocket
            THttpClient TIOStreamTransport
            TFramedTransport TFastFramedTransport
            TServerSocket TNonblockingServerSocket]
           [java.io InputStream OutputStream]
           [java.net InetSocketAddress]))

(declare tcp http streams)

;; ## Protocol for "predefined" Transport Schemes
;;
;; `TTransport` is taken as-is, Integers are used as a local TCP
;; port, a vector is used as TCP host/port pair.

(defprotocol TransportConvertable
  "Protocol for Things convertable to TTransport."
  (to-transport [this]))

(defn ->transport
  "Create transport from TransportConvertable"
  ^TTransport
  [t]
  (to-transport t))

(extend-type TTransport
  TransportConvertable
  (to-transport [this] this))

;; Integers are interpreted as TCP port numbers on
;; localhost.

(extend-protocol TransportConvertable
  java.lang.Byte
  (to-transport [this]
    (tcp (int this)))
  java.lang.Short
  (to-transport [this]
    (tcp (int this)))
  java.lang.Integer
  (to-transport [this]
    (tcp this))
  java.lang.Long
  (to-transport [this]
    (tcp (int this))))

;; Strings are interpreted either as URLs ("http://...") or as TCP hosts
;; ("host:port").

(extend-type java.lang.String
  TransportConvertable
  (to-transport [this]
    (if (.startsWith this "http://")
      (http this)
      (if-let [idx (.indexOf this ":")]
        (try
          (let [host (.substring this 0 idx)
                port (Integer/parseInt (.substring this (inc idx)))]
            (tcp host port))
          (catch Exception ex
            (throw (Exception. (str "Cannot convert String to TTransport: " this) ex))))
        (throw (Exception. (str "Cannot convert String to TTransport: " this)))))))

;; Vectors are either interpreted as streams (`[in out]` and `[out in]`) or as TCP hosts
;; (`[host port]`)

(extend-type clojure.lang.IPersistentVector
  TransportConvertable
  (to-transport [this]
    (when-not (seq this)
      (throw (Exception. "Cannot convert empty Vector to TTransport.")))
    (condp = (count this)
      1 (to-transport (first this))
      2 (let [[a b] this]
          (cond (instance? InputStream a) (streams a b)
                (instance? OutputStream a) (streams b a)
                (and (string? a) (integer? b)) (tcp a b)
                :else (throw (Exception. (str "Cannot convert Vector to TTransport: " this)))))
      (throw (Exception. (str "Cannot convert Vector to TTransport: " this))))))

;; ## Transport Wrappers

(defn framed
  "Wrap transport with framed transport (prefixes messages with 4 byte frame size)."
  ^TTransport
  [t & {:keys[max-frame-length]}]
  (if max-frame-length
    (TFramedTransport. (->transport t) (int max-frame-length))
    (TFramedTransport. (->transport t))))

(defn fast-framed
  "Wrap transport with fast framed transport (compatible with `framed`, but using
   persistent byte buffers)."
  ^TTransport
  [t & {:keys[max-frame-length initial-buffer-size]}]
  (TFastFramedTransport.
    (->transport t)
    (int (or initial-buffer-size TFastFramedTransport/DEFAULT_BUF_CAPACITY))
    (int (or max-frame-length TFastFramedTransport/DEFAULT_MAX_LENGTH))))

;; ## Client Transports

(defn tcp
  "Create TCP transport."
  (^TTransport [port] (tcp "localhost" port))
  (^TTransport [host port] (TSocket. (str host) (int port))))

(defn tcp-async
  "Create non-blocking TCP transport."
  (^TTransport [port] (tcp-async "localhost" port))
  (^TTransport [host port] (TNonblockingSocket. (str host) (int port))))

(defn http
  "Create HTTP transport."
  ^TTransport
  [url & {:keys[connect-timeout read-timeout custom-headers]}]
  (let [^THttpClient c (THttpClient. (str url))]
    (when (integer? connect-timeout)
      (.setConnectTimeout c (int connect-timeout)))
    (when (integer? read-timeout)
      (.setReadTimeout c (int read-timeout)))
    (when (map? custom-headers)
      (.setCustomHeaders c custom-headers))
    c))

(defn streams
  "Create IOStream transport."
  ^TTransport
  [^InputStream in ^OutputStream out]
  (TIOStreamTransport. in out))

;; ## Server Transports

(defn blocking-server-transport
  "Create blocking server transport on the given port."
  ^TServerSocket
  [port {:keys[bind client-timeout]}]
  (if bind
    (let [^InetSocketAddress addr (InetSocketAddress. (str bind) (int port))]
      (TServerSocket. addr (int (or client-timeout 0))))
    (TServerSocket. (int port) (int (or client-timeout 0)))))

(defn nonblocking-server-transport
  "Create non-blocking server transport on the given port."
  ^TNonblockingServerSocket
  [port {:keys[bind client-timeout]}]
  (if bind
    (let [^InetSocketAddress addr (InetSocketAddress. (str bind) (int port))]
      (TNonblockingServerSocket. addr (int (or client-timeout 0))))
    (TNonblockingServerSocket. (int port) (int (or client-timeout 0)))))
