(ns ^{ :doc "Thrift Protocol Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.protocol.core
  (:import [org.apache.thrift.protocol 
            TProtocol TProtocolFactory
            TBinaryProtocol$Factory TCompactProtocol$Factory
            TJSONProtocol$Factory TSimpleJSONProtocol$Factory TTupleProtocol$Factory]
           [org.apache.thrift.transport TTransport]))

;; ## Protocol Multimethod

(defmulti ^TProtocolFactory protocol-factory*
  "Create Protocol Factory of the given Type with the given Options."
  (fn [id _] id)
  :default nil)

(defmethod protocol-factory* nil
  [id _]
  (throw (Exception. (str "Unknown Protocol: " id))))

(defn ^TProtocolFactory protocol-factory
  "Create Protocol Factory of the given Type with the given Options."
  [id & args]
  (protocol-factory* id (apply hash-map args)))

(defn ^TProtocol protocol
  "Create Protocol of the given Type with the given Options."
  [id ^TTransport transport & args]
  (when-let [^TProtocolFactory factory (apply protocol-factory id args)]
    (.getProtocol factory transport)))

;; ## Protocol Implementations

(defmethod protocol-factory* :binary
  [_ {:keys[strict-read strict-write]}]
  (TBinaryProtocol$Factory. 
    (boolean strict-read)
    (boolean strict-write)))

(defmethod protocol-factory* :compact
  [_ {:keys[max-network-bytes]}]
  (TCompactProtocol$Factory.
    (long (or max-network-bytes -1))))

(let [json-factory (TJSONProtocol$Factory.)]
  (defmethod protocol-factory* :json
    [_ _]
    json-factory))

(let [simple-json-factory (TSimpleJSONProtocol$Factory.)]
  (defmethod protocol-factory* :simple-json
    [_ _]
    simple-json-factory))

(let [tuple-factory (TTupleProtocol$Factory.)]
  (defmethod protocol-factory* :tuple
    [_ _]
    tuple-factory))
