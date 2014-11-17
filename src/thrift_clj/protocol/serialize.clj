(ns ^{ :doc "Thrift Type Serialization/Deserialization"
       :author "Yannick Scherer" }
  thrift-clj.protocol.serialize
  (:use [thrift-clj.protocol.core :only [protocol-factory]])
  (:require [thrift-clj.gen.core :as c])
  (:import [org.apache.thrift TSerializer TDeserializer]))

;; ## Serialize/Deserialize Protocols

(defprotocol Serializer
  "Protocol for Serialization."
  (value->bytes [this v])
  (value->string [this v] [this v charset]))

(defprotocol Deserializer
  "Protocol for Deserialization."
  (bytes->value [this prototype data])
  (string->value [this prototype string] [this prototype string charset]))

(extend-type TSerializer
  Serializer
  (value->bytes [this v]
    (.serialize this (c/->thrift v)))
  (value->string
    ([this v] (.toString this (c/->thrift v)))
    ([this v charset] (.toString this (c/->thrift v) charset))))

(extend-type TDeserializer
  Deserializer
  (bytes->value [this prototype data]
    (let [proto (c/->thrift-unchecked prototype)]
      (.deserialize this proto data)
      (c/->clj proto)))
  (string->value
    ([this prototype string]
     (let [proto (c/->thrift-unchecked prototype)]
       (.fromString this proto string)
       (c/->clj proto)))
    ([this prototype string charset]
     (let [proto (c/->thrift-unchecked prototype)]
       (.deserialize this proto string charset)
       (c/->clj proto)))))

;; ## Combined Serializer/Deserializer

(deftype Serialize [serializer deserializer]
  Serializer
  (value->bytes [this v]
    (value->bytes serializer v))
  (value->string [this v]
    (value->string serializer v))
  (value->string [this v charset]
    (value->string serializer v charset))

  Deserializer
  (bytes->value [this prototype data]
    (bytes->value deserializer prototype data))
  (string->value [this prototype string]
    (string->value deserializer prototype string))
  (string->value [this prototype string charset]
    (string->value deserializer prototype string charset)))

;; ## Serializer Creation

(defn serializer
  "Create new Serialization Helper using the given Protocol Type and Options."
  [protocol-id & args]
  (let [factory (apply protocol-factory protocol-id args)]
    (Serialize.
      (TSerializer. factory)
      (TDeserializer. factory))))
