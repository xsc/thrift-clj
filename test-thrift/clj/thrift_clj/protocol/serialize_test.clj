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

(def person-clj (Person. (Name. "Some" "One") nil))
(def person-thr (thrift/->thrift person-clj))
(def proto-clj (Person. nil nil))
(def proto-thr (thriftclj.structs.Person.))
(def byte-class (Class/forName "[B"))

;; ## Tests

(fact "about unknown protocol"
  (serializer ::unknown) => (throws Exception))

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
  ?protocol           ?value           ?target
  :binary             person-clj       proto-clj
  :binary             person-clj       proto-thr
  :binary             person-thr       proto-clj
  :binary             person-thr       proto-thr
  :compact            person-clj       proto-clj
  :compact            person-clj       proto-thr
  :compact            person-thr       proto-clj
  :compact            person-thr       proto-thr
  :tuple              person-clj       proto-clj
  :tuple              person-clj       proto-thr
  :tuple              person-thr       proto-clj
  :tuple              person-thr       proto-thr
  :json               person-clj       proto-clj
  :json               person-clj       proto-thr
  :json               person-thr       proto-clj
  :json               person-thr       proto-thr
  )


(tabular
  (fact "about serializable/deserializable protocols"
    (let [s (serializer ?protocol)
          data-bytes (value->bytes s ?value)
          data-string (value->string s ?value)]
      s => truthy
      data-bytes => #(instance? byte-class %)
      data-string => string?
      (bytes->value s ?target data-bytes) => (thrift/->clj ?target)
      (string->value s ?target data-string) => (thrift/->clj ?target)))
  ?protocol           ?value           ?target
  :simple-json        person-clj       proto-clj
  :simple-json        person-clj       proto-thr
  :simple-json        person-thr       proto-clj
  :simple-json        person-thr       proto-thr)
