(ns ^{ :doc "Service/Client interaction using different Protocols."
       :author "Yannick Scherer" }
  thrift-clj.rpc.rpc-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet
        thrift-clj.rpc.fixtures))

(thrift/import
  (:types [thriftclj.structs Person]
          [thriftclj.exceptions StorageError])
  (:clients [thriftclj.services.TelephoneBook :as TB]))

;; ## Blocking

(tabular
  (tabular
    (let [port (int (+ 10000 (rand-int 10000)))
          server (?server telephone-book port :bind "localhost" :protocol ?proto)]
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
            (let [r (TB/findByLocation c location-clj)]
              r => set?
              r =not=> empty?
              r => #(every? (partial instance? Person) %))))))
    ?server
    thrift/single-threaded-server
    thrift/multi-threaded-server)
  ?proto :binary :compact :json :tuple)

;; ## Non-Blocking

(tabular
  (let [port (int (+ 10000 (rand-int 10000)))
        server (thrift/nonblocking-server telephone-book port :bind "localhost" :protocol ?proto)]
    (with-state-changes [(before :facts (thrift/serve! server))
                         (after :facts (thrift/stop! server))]
      (fact "about calling a service on localhost"
        (with-open [c (thrift/connect! TB (thrift/framed ["localhost" port]) :protocol ?proto)]
          (TB/storePerson c person-clj) => truthy
          (TB/storePerson c person-thr) => truthy
          (let [r (TB/findByName c "Some" "One")]
            r => set?
            r =not=> empty?
            r => #(every? (partial instance? Person) %))
          (let [r (TB/findByLocation c location-clj)]
            r => set?
            r =not=> empty?
            r => #(every? (partial instance? Person) %))))))
  ?proto :binary :compact :json :tuple)
