(ns ^{ :doc "Client Transports"
       :author "Yannick Scherer" }
  thrift-clj.client.transport
  (:import [org.apache.thrift.transport
            TTransport TSocket TNonblockingSocket THttpClient
            TFramedTransport TFastFramedTransport
            TMemoryInputTransport TMemoryBuffer]))

;; ## Creation Multimethod

(defmulti ^TTransport create-client-transport 
  "Create Client Transport using a Type ID and optional arguments."
  (fn [id & args] id)
  :default nil)

(defmethod create-client-transport nil
  [id & _]
  (throw (Exception. (str "No such transport: " id))))

;; ## Transport Types

(defmethod create-client-transport :socket
  [_ host port]
  (TSocket. (str host) (int port)))

(defmethod create-client-transport :nonblocking
  [_ host port]
  (TNonblockingSocket. (str host) (int port)))

(defmethod create-client-transport :http
  [_ url & {:keys[connect-timeout read-timeout custom-headers]}]
  (let [^THttpClient c (THttpClient. (str url))]
    (when (integer? connect-timeout)
      (.setConnectTimeout c (int connect-timeout)))
    (when (integer? read-timeout)
      (.setReadTimeout c (int read-timeout)))
    (when (map? custom-headers)
      (.setCustomHeaders c custom-headers))
    c))

(defmethod create-client-transport :framed
  [_ & rest-transport]
  (let [inner (apply create-client-transport rest-transport)]
    (TFramedTransport. inner)))

(defmethod create-client-transport :fast-framed
  [_ & rest-transport]
  (let [inner (apply create-client-transport rest-transport)]
    (TFastFramedTransport. inner)))

(defmethod create-client-transport :memory-in
  [_ data]
  (TMemoryInputTransport. (bytes data)))

(defmethod create-client-transport :memory-out
  ([_] (TMemoryBuffer. 1024))
  ([_ size] (TMemoryBuffer. (int size)))) 
