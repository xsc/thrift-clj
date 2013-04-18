(ns ^{ :doc "Client Transports"
       :author "Yannick Scherer" }
  thrift-clj.client.transport
  (:import [org.apache.thrift.transport
            TTransport TSocket TNonblockingSocket THttpClient
            TIOStreamTransport]
           [java.io InputStream OutputStream]))

;; ## Transports

(defn ^TTransport tcp
  "Create TCP transport."
  ([port] (tcp "localhost" port))
  ([host port] (TSocket. (str host) (int port))))

(defn ^TTransport tcp-async
  "Create non-blocking TCP transport."
  ([port] (tcp-async "localhost" port))
  ([host port] (TNonblockingSocket. (str host) (int port))))

(defn ^TTransport http
  "Create HTTP transport."
  [url & {:keys[connect-timeout read-timeout custom-headers]}]
  (let [^THttpClient c (THttpClient. (str url))]
    (when (integer? connect-timeout)
      (.setConnectTimeout c (int connect-timeout)))
    (when (integer? read-timeout)
      (.setReadTimeout c (int read-timeout)))
    (when (map? custom-headers)
      (.setCustomHeaders c custom-headers))
    c))

(defn ^TTransport streams
  "Create IOStream transport."
  [^InputStream in ^OutputStream out]
  (TIOStreamTransport. in out))

;; ## Protocol for "predefined" Transport Schemes
;;
;; `TTransport` is taken as-is, Integers are used as a local TCP
;; port, a vector is used as TCP host/port pair.

(defprotocol TransportConvertable
  "Protocol for Things convertable to TTransport."
  (to-transport [this]))

(extend-type TTransport
  TransportConvertable
  (to-transport [this] this))

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
