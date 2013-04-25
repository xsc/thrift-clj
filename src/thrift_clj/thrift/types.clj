(ns ^{ :doc "Thrift Type Analysis"
       :author "Yannick Scherer" }
  thrift-clj.thrift.types
  (:import (java.lang.reflect Field)
           (org.apache.thrift TBase TEnum TFieldIdEnum TException)
           (org.apache.thrift.meta_data FieldMetaData FieldValueMetaData)))

;; ## Types

;; ### Helpers

(def ^:private THRIFT_TYPES
  "Mapping Thrift Type IDs to Type Keywords.
   (see: `org.apache.thrift.protocol.TType`)"
  { 0x0 :stop   0x1 :void   0x2 :bool 
    0x3 :byte   0x4 :double 0x6 :i16
    0x8 :i32    0xa :i64    0xb :string
    0xc :struct 0xd :map    0xe :set
    0xf :list })

(defmulti ^:private extend-field-metadata-map
  "Type-specific Extension of Metadata Map."
  (fn [metadata-map ^FieldValueMetaData _] 
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

;; ### Type-specific Handling

(defmethod extend-field-metadata-map :bool
  [m _]
  (assoc m :wrapper `boolean))

(defmethod extend-field-metadata-map :byte
  [m _]
  (assoc m :wrapper `byte))

(defmethod extend-field-metadata-map :i16
  [m _]
  (assoc m :wrapper `short))

(defmethod extend-field-metadata-map :i32
  [m _]
  (assoc m :wrapper `int))

(defmethod extend-field-metadata-map :i64
  [m _]
  (assoc m :wrapper `long))

(defmethod extend-field-metadata-map :string
  [m _]
  (assoc m :wrapper `str))

(defmethod extend-field-metadata-map :set
  [m _]
  (assoc m :wrapper `set))

(defmethod extend-field-metadata-map :list
  [m _]
  (assoc m :wrapper `vec))

;; ### Metadata

(defn- create-field-metadata-map
  "Create Metadata for Field."
  [^TFieldIdEnum k ^FieldMetaData metadata-obj]
  (let [^FieldValueMetaData value-metadata-obj (.-valueMetaData metadata-obj)
        metadata-map (-> {}
                       (assoc :key k)
                       (assoc :id (.getThriftFieldId k))
                       (assoc :name (.-fieldName metadata-obj))
                       (assoc :require (THRIFT_REQUIREMENT_TYPES (.-requirementType metadata-obj)))
                       (assoc :type (THRIFT_TYPES (.-type value-metadata-obj))))]
    (extend-field-metadata-map metadata-map value-metadata-obj)))

(def thrift-type-metadata
  "Get Seq of Type Field Metadata Maps."
  (let [prototype (java.util.HashMap.)]
    (fn [^Class class]
      (try
        (when-let [^Field f (.getDeclaredField class "metaDataMap")]
          (let [^java.util.Map m (.get f prototype)]
            (map
              (fn [[k metadata-obj]]
                (create-field-metadata-map k metadata-obj))
              m)))
        (catch Exception _ nil)))))

;; ### Checks

(defn thrift-struct?
  "Check if Class represents a Thrift Type/Struct."
  [^Class class]
  (and (.isAssignableFrom TBase class)
       (nil? (.getDeclaringClass class))))

(defn thrift-enum?
  "Check if Class represents a Thrift Enum."
  [^Class class]
  (.isAssignableFrom TEnum class))

(defn thrift-exception?
  "Check if Class represents a Thrift Exception."
  [^Class class]
  (.isAssignableFrom TException class))
