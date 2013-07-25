(ns ^{ :doc "Tests for Service Interface Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.iface-test
  (:use midje.sweet)
  (:require [thrift-clj.core :as thrift]))

;; ## Import

(thrift/import-types
  [thriftclj.structs Person Name Location Country])
(thrift/import-iface 
  [thriftclj.services.TelephoneBook :as TB])

;; ## Fixtures

(def person-clj (Person. (Name. "Some" "One") nil false))
(def person-thr (thrift/->thrift person-clj))
(def location-clj (Location. 12345 "City" Country/US))
(def location-thr (thrift/->thrift location-clj))
(def ^java.util.List result-list [person-thr])

;; ## Dummy Implementation

(deftype Dummy []
  thriftclj.services.TelephoneBook$Iface
  (storePerson [this p]
    (instance? thriftclj.structs.Person p))
  (findByName [this firstName lastName]
    (java.util.HashSet. result-list))
  (findByLocation [this loc]
    (java.util.HashSet. result-list)))

(def ^Dummy telephone-book (Dummy.))

;; ## Tests

(fact "about Iface import completeness"
  TB/storePerson => fn?
  TB/findByName => fn?
  TB/findByLocation => fn?)

(fact "about direct Iface implementation"
  telephone-book => #(instance? thriftclj.services.TelephoneBook$Iface %)
  (.storePerson telephone-book person-thr) => truthy
  (.storePerson telephone-book person-clj) => (throws Exception)
  (tabular
    (fact "findBy*"
      (let [ps ?c]
        ps => truthy
        ps => #(instance? java.util.Set %)
        ps =not=> empty?
        ps => #(every? (partial instance? thriftclj.structs.Person) %)))
    ?c
    (.findByName telephone-book "Some" "One") 
    (.findByLocation telephone-book location-thr))
  (.findByLocation telephone-book location-clj) => (throws Exception))

(fact "about transparent input and output type conversion"
  (fact "about fixtures"
    (class person-clj) =not=> thriftclj.structs.Person
    (class person-thr) => thriftclj.structs.Person
    (class location-clj) =not=> thriftclj.structs.Location
    (class location-thr) => thriftclj.structs.Location)
  (fact "about input"
    (TB/storePerson telephone-book person-clj) => truthy
    (TB/storePerson telephone-book person-thr) => truthy
    (TB/storePerson telephone-book (Object.)) => (throws Exception))
  (fact "about output"
    (let [ps (TB/findByName telephone-book "Some" "One")]
      ps => truthy
      ps => set?
      ps =not=> empty?
      ps => #(every? (partial instance? Person) %)))
  (tabular
    (fact "about input and output"
      (let [ps (TB/findByLocation telephone-book ?loc)]
        ps => truthy
        ps => set?
        ps =not=> empty?
        ps => #(every? (partial instance? Person) %)))
    ?loc
    location-clj
    location-thr)
  (TB/findByLocation telephone-book (Object.)) => (throws Exception))
