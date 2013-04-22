(ns ^{ :doc "Service/Client interaction using different Protocols."
       :author "Yannick Scherer" }
  thrift-clj.rpc-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet))

;; ## Import

(thrift/import
  (:types [thriftclj.structs Person Name Location Country]
          [thriftclj.exceptions StorageError])
  (:services [thriftclj.services.TelephoneBook :as TBService])
  (:clients [thriftclj.services.TelephoneBook :as TB]))

;; ## Fixtures

(def person-clj (Person. (Name. "Some" "One") nil))
(def person-thr (thrift/->thrift person-clj))
(def port (int (+ 40000 (rand-int 10000))))

(def error-person-clj (Person. (Name. "Some" "Two") nil))
(def error-person-thr (thrift/->thrift error-person-clj))

(thrift/defservice telephone-book
  TBService
  (storePerson [{:keys[name]}]
    (when (= (:lastName name) "Two")
      (thrift/throw (StorageError. "Stupid Name.")))
     true)
  (findByName [_ _] #{person-clj})
  (findByLocation [_] #{person-clj}))

;; ## Tests

(tabular 
  (tabular
    (let [server (?server telephone-book port :bind "localhost" :protocol ?proto)]
      (with-state-changes [(before :facts (thrift/serve! server))
                           (after :facts (thrift/stop! server))]
        (fact "about calling a service on localhost"
          (with-open [c (thrift/connect! TB ["localhost" port] :protocol ?proto)]
            (TB/storePerson c person-clj) => truthy
            (TB/storePerson c person-thr) => truthy
            (let [r (TB/findByName c "Some" "One")]
              r => set?
              r =not=> empty?
              r => #(every? (partial instance? Person) %))
            (let [r (TB/findByLocation c (Location. 12345 "City" Country/US))]
              r => set?
              r =not=> empty?
              r => #(every? (partial instance? Person) %))))))
    ?server
    thrift/single-threaded-server
    thrift/multi-threaded-server)
  ?proto :binary :compact :json :tuple)

(tabular 
  (tabular
    (let [server (?server telephone-book port :bind "localhost" :protocol ?proto)]
      (with-state-changes [(before :facts (thrift/serve! server))
                           (after :facts (thrift/stop! server))]
        (fact "about exception transfer"
          (with-open [c (thrift/connect! TB ["localhost" port] :protocol ?proto)]
            (thrift/try 
              (TB/storePerson c error-person-clj)
              (catch StorageError s
                s)) => (StorageError. "Stupid Name.")
            (thrift/try 
              (TB/storePerson c error-person-thr)
              (catch StorageError s
                s)) => (StorageError. "Stupid Name.")))))
    ?server
    thrift/single-threaded-server
    thrift/multi-threaded-server)
  ?proto :binary :compact :json :tuple)
