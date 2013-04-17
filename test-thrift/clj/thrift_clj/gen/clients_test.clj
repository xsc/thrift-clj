(ns ^{ :doc "Tests for Thrift Client Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.clients-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet))

;; ## Import

(thrift/import-types
  [thriftclj.structs Person Name Location Country])
(thrift/import-services 
  thriftclj.services.TelephoneBook)
(thrift/import-clients
  [thriftclj.services.TelephoneBook :as TB])

;; ## Fixtures

(def person-clj (Person. (Name. "Some" "One") nil))
(def person-thr (thrift/->thrift person-clj))

;; ## Tests

(fact "about the imported service interface"
  TB/storePerson => fn?
  TB/findByName => fn?
  TB/findByLocation => fn?)
