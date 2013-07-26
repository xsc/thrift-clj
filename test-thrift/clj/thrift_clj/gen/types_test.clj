(ns
  thrift-clj.gen.types-test
  (:use midje.sweet)
  (:require [thrift-clj.core :as thrift]))

(thrift/import-types
  [thriftclj.structs Name Country Location Person People])

(fact "about structs with primitive fields"
  (let [cn (Name. "Some" "One")
        ^thriftclj.structs.Name tn (thrift/->thrift cn)
        rn (thrift/->clj tn)]
    (class tn) => thriftclj.structs.Name
    (class rn) => (class cn)
    (.isSetFirstName tn) => truthy
    (.isSetLastName tn) => truthy
    (.getFirstName tn) => (:firstName cn)
    (.getLastName tn) => (:lastName cn))
  (let [cn (Name. nil "One")
        ^thriftclj.structs.Name tn (thrift/->thrift cn)]
    (class tn) => thriftclj.structs.Name
    (.isSetFirstName tn) => falsey)
  (let [cn (Name. nil nil)]
    (thrift/->thrift cn) => (throws Exception)))

(fact "about structs with structured fields"
  (let [cp (Person. (Name. "Some" "One") nil false)
        ^thriftclj.structs.Person tp (thrift/->thrift cp)
        rp (thrift/->clj tp)
        cn (:name cp)
        tn (.getName tp)
        rn (:name rp)]
    (class tp) => thriftclj.structs.Person
    (class tn) => thriftclj.structs.Name
    (class rp) => (class cp)
    (class rn) => (class cn)
    (.getFirstName tn) => (:firstName cn)
    (.getLastName tn) => (:lastName cn)
    (.isSetLocation tp) => falsey
    (.isLiving tp) => falsey))

(fact "about how enums are imported directly"
  (class Country/DE) => thriftclj.structs.Country
  (class Country/AT) => thriftclj.structs.Country
  (class Country/US) => thriftclj.structs.Country
  (class Country/GB) => thriftclj.structs.Country
  (Country/findByValue 0) => Country/DE
  (Country/findByValue 1) => Country/AT
  (Country/findByValue 2) => Country/US
  (Country/findByValue 3) => Country/GB)

(fact "about structs with enum fields"
  (let [cl (Location. 12345 "City" Country/US)
        ^thriftclj.structs.Location tl (thrift/->thrift cl)
        cc (:country cl)
        tc (.getCountry tl)]
    (class tl) => thriftclj.structs.Location
    (class cc) => thriftclj.structs.Country
    (class tc) => thriftclj.structs.Country
    cc => Country/US
    tc => Country/US))

(fact "about structs with set fields"
  (let [cpl (People. (set (repeat 5 (Person. (Name. "Some" "One") nil false))) nil nil)
        ^thriftclj.structs.People tpl (thrift/->thrift cpl)
        rpl (thrift/->clj tpl)]
    (.getPeopleSet tpl) => #(instance? java.util.Set %)
    (.getPeopleSet tpl) => #(every? (partial instance? thriftclj.structs.Person) %)
    (:peopleSet cpl) => #(every? (partial instance? Person) %)
    (:peopleSet rpl) => #(every? (partial instance? Person) %)))

(fact "about structs with list fields"
  (let [cpl (People. nil (vec (repeat 5 (Person. (Name. "Some" "One") nil false))) nil)
        ^thriftclj.structs.People tpl (thrift/->thrift cpl)
        rpl (thrift/->clj tpl)]
    (.getPeopleList tpl) => #(instance? java.util.List %)
    (.getPeopleList tpl) => #(every? (partial instance? thriftclj.structs.Person) %)
    (:peopleList cpl) => #(every? (partial instance? Person) %)
    (:peopleList rpl) => #(every? (partial instance? Person) %)))

(fact "about structs with map fields"
  (let [cpl (People. nil nil (into {} (repeat 5 [0 (Person. (Name. "Some" "One") nil false)])))
        ^thriftclj.structs.People tpl (thrift/->thrift cpl)
        rpl (thrift/->clj tpl)]
    (.getPeopleMap tpl) => #(instance? java.util.Map %)
    (keys (.getPeopleMap tpl)) => #(every? integer? %)
    (vals (.getPeopleMap tpl)) => #(every? (partial instance? thriftclj.structs.Person) %)
    (keys (:peopleMap cpl)) => #(every? integer? %)
    (vals (:peopleMap cpl)) => #(every? (partial instance? Person) %)
    (keys (:peopleMap rpl)) => #(every? integer? %)
    (vals (:peopleMap rpl)) => #(every? (partial instance? Person) %)))

(fact "about instantiating records with factory functions"
  (let [positional (->Location 12345 "City" Country/US)
        map-args (map->Location {:zip 12345 :city "City" :country Country/US})
        standard (Location. 12345 "City" Country/US)]
    positional => standard
    map-args => standard))
