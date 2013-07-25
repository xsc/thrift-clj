(ns ^{ :doc "Tests for Serialization"
       :author "Yannick Scherer" }
  thrift-clj.protocol.serialize-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet
        thrift-clj.protocol.serialize))

;; ## Import

(thrift/import-types 
  [thriftclj.structs Person Name])

;; ## Fixtures

(def person-clj (Person. (Name. "Some" "One") nil false))
(def person-thr (thrift/->thrift person-clj))
(def proto-clj (Person. nil nil false))
(def proto-thr (thriftclj.structs.Person.))
(def byte-class (Class/forName "[B"))

;; ## Tests

(fact "about unknown protocol"
  (serializer ::unknown) => (throws Exception))

(tabular
  (tabular
    (tabular
      (fact "about serializable/deserializable protocols"
        (let [s (serializer ?protocol)
              data-bytes (value->bytes s ?value)
              data-string (value->string s ?value)]
          s => truthy
          data-bytes => #(instance? byte-class %)
          data-string => string?
          (bytes->value s ?target data-bytes) => (thrift/->clj ?value)
          (string->value s ?target data-string) => (thrift/->clj ?value)))
      ?target proto-clj proto-thr)
    ?value person-clj person-thr)
  ?protocol :binary :compact :json :tuple)

(tabular
  (tabular
    (tabular
      (fact "about serialize-only protocols"
        (let [s (serializer ?protocol)
              data-bytes (value->bytes s ?value)
              data-string (value->string s ?value)]
          s => truthy
          data-bytes => #(instance? byte-class %)
          data-string => string?
          (bytes->value s ?target data-bytes) => (thrift/->clj ?target)
          (string->value s ?target data-string) => (thrift/->clj ?target)))
      ?target proto-clj proto-thr)
    ?value person-clj person-thr)
  ?protocol :simple-json)
