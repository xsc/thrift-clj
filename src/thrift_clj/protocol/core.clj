(ns ^{ :doc "Thrift Protocol Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.protocol.core
  (:import [org.apache.thrift.protocol
            TProtocol TProtocolFactory
            TBinaryProtocol$Factory TCompactProtocol$Factory
            TJSONProtocol$Factory TSimpleJSONProtocol$Factory]
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

;; ## Compatibility Helpers

(defmacro when-protocol
  [class-name & body]
  (when (try
          (Class/forName (str "org.apache.thrift.protocol." class-name))
          (catch ClassNotFoundException _))
    `(do ~@body)))

(defmacro construct
  [class-name & argvs]
  (let [n (str "org.apache.thrift.protocol." class-name)
        c (Class/forName n)
        matches-arity? (comp
                         (->> (.getConstructors c)
                              (map #(.getParameterTypes
                                      ^java.lang.reflect.Constructor %))
                              (map alength)
                              (set))
                         count)
        argv (some #(when (matches-arity? %) %) argvs)]
    (assert argv "no matching constructor found.")
    `(new ~(symbol n) ~@argv)))

;; ## Protocol Implementations

(defmethod protocol-factory* :binary
  [_ {:keys[strict-read strict-write]}]
  (TBinaryProtocol$Factory.
    (boolean strict-read)
    (boolean strict-write)))

(defmethod protocol-factory* :compact
  [_ {:keys[max-network-bytes]}]
  (construct
    TCompactProtocol$Factory
    [(long (or max-network-bytes -1))]
    []))

(let [json-factory (TJSONProtocol$Factory.)]
  (defmethod protocol-factory* :json
    [_ _]
    json-factory))

(let [simple-json-factory (TSimpleJSONProtocol$Factory.)]
  (defmethod protocol-factory* :simple-json
    [_ _]
    simple-json-factory))

(defmethod protocol-factory* :tuple
  [_ _]
  (println "WARN: tuple protocol not supported - falling back to compact.")
  (protocol-factory* :compact {}))

(when-protocol
  TTupleProtocol
  (let [tuple-factory (org.apache.thrift.protocol.TTupleProtocol$Factory.)]
    (defmethod protocol-factory* :tuple
      [_ _]
      tuple-factory)))
