(ns ^{ :doc "Tests for Thrift Client Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.clients-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet))

(thrift/import-types
  [thriftclj.structs Person Name Location Country])
(thrift/import-clients
  [thriftclj.services.TelephoneBook :as TB])

(fact "about imported interface"
  TB/storePerson => fn?
  TB/findByName => fn?
  TB/findByLocation => fn?)
