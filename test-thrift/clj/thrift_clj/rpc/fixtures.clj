(ns ^{ :doc "Fixtures for RPC Tests."
       :author "Yannick Scherer" }
  thrift-clj.rpc.fixtures
  (:require [thrift-clj.core :as thrift]))

;; ## Import

(thrift/import
  (:types [thriftclj.structs Person Name Location Country]
          [thriftclj.exceptions StorageError])
  (:services [thriftclj.services.TelephoneBook :as TBService]))

;; ## Fixtures

(def person-clj (Person. (Name. "Some" "One") nil false))
(def person-thr (thrift/->thrift person-clj))
(def error-person-clj (Person. (Name. "Some" "Two") nil false))
(def error-person-thr (thrift/->thrift error-person-clj))
(def location-clj (Location. 1234 "City" Country/US))

(thrift/defservice telephone-book
  TBService
  (storePerson [{:keys[name]}]
    (when (= (:lastName name) "Two")
      (thrift/throw (StorageError. "Stupid Name.")))
     true)
  (findByName [_ _] #{person-clj})
  (findByLocation [_] #{person-clj}))
