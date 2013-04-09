(ns ^{ :doc "Wrapping Thrift Data Structures"
       :author "Yannick Scherer" }
  thrift-clj.thrift
  (:import(java.lang.reflect Field)
           (org.apache.thrift.meta_data FieldMetaData FieldValueMetaData)))

;; ## Types

;; ### Helpers

(def ^:private THRIFT_TYPES
  "Mapping Thrift Type IDs to Type Keywords.
   (see: `org.apache.thrift.protocol.TType`)"
  { 0x0 :stop   0x1 :void   0x2 :bool 
    0x3 :byte   0x4 :double 0x6 :i16
    0x7 :i32    0x8 :i64    0xb :string
    0xc :struct 0xd :map    0xe :set
    0xf :list })

(defmulti ^:private extend-field-metadata-map
  "Type-specific Extension of Metadata Map."
  (fn [metadata-map ^FieldValueMetaData metadata-obj] 
    (:type metadata-map))
  :default nil)

(defmethod extend-field-metadata-map nil
  [metadata-map _]
  metadata-map)

(def ^:private THRIFT_REQUIREMENT_TYPES
  "Mapping Thrift Requirement Type IDs to Keywords."
  { 1 :required
    2 :optional
    3 :default })

;; ### Metadata

(defn- create-field-metadata-map
  "Create Metadata for Field."
  [k ^FieldMetaData metadata-obj]
  (let [value-metadata-obj (.-valueMetaData metadata-obj)
        metadata-map (-> {}
                       (assoc :key k)
                       (assoc :id (.getThriftFieldId k))
                       (assoc :name (.-fieldName metadata-obj))
                       (assoc :require (THRIFT_REQUIREMENT_TYPES (.-requirementType metadata-obj)))
                       (assoc :type (THRIFT_TYPES (.-type value-metadata-obj))))]
    (extend-field-metadata-map
      metadata-map 
      value-metadata-obj)))

(def type-metadata
  "Get Seq of Type Field Metadata Maps."
  (let [prototype (java.util.HashMap.)]
    (fn [^Class class]
      (when-let [^Field f (.getDeclaredField class "metaDataMap")]
        (let [^java.util.Map m (.get f prototype)]
          (map
            (fn [[k metadata-obj]]
              (create-field-metadata-map k metadata-obj))
            m))))))
