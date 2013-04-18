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

(thrift/defservice telephone-book
  TelephoneBook
  (storePerson [_] true)
  (findByName [_ _] #{person-clj})
  (findByLocation [_] #{person-clj}))

;; ## Tests

(fact "about the imported service interface"
  TB/storePerson => fn?
  TB/findByName => fn?
  TB/findByLocation => fn?)

(tabular 
  (tabular
    (fact "about calling remote services"
      (let [port (+ 10000 (rand-int 10000))
            server (?server telephone-book port :protocol ?proto)]
        (thrift/serve! server)
        (with-open [c (thrift/connect! TB port :protocol ?proto)]
          (TB/storePerson c person-clj) => truthy
          (TB/storePerson c person-thr) => truthy
          (let [r (TB/findByName c "Some" "One")]
            r => set?
            r =not=> empty?
            r => #(every? (partial instance? Person) %))
          (let [r (TB/findByLocation c (Location. 12345 "City" Country/US))]
            r => set?
            r =not=> empty?
            r => #(every? (partial instance? Person) %)))
        (thrift/stop! server)))
    ?server
    thrift/single-threaded-server
    thrift/multi-threaded-server)
  ?proto :binary :compact :json :tuple)
