(ns ^{ :doc "Testing Exception Transfer"
       :author "Yannick Scherer" }
  thrift-clj.rpc.exception-test
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
        (fact "about exception transfer in blocking servers"
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

;; ## Non-Blocking

(tabular
  (let [port (int (+ 10000 (rand-int 10000)))
        server (thrift/nonblocking-server telephone-book port :bind "localhost" :protocol ?proto)]
    (with-state-changes [(before :facts (thrift/serve! server))
                         (after :facts (thrift/stop! server))]
      (fact "about exception transfer in non-blocking servers"
        (with-open [c (thrift/connect! TB (thrift/framed ["localhost" port]) :protocol ?proto)]
          (thrift/try
            (TB/storePerson c error-person-clj)
            (catch StorageError s
              s)) => (StorageError. "Stupid Name.")
          (thrift/try
            (TB/storePerson c error-person-thr)
            (catch StorageError s
              s)) => (StorageError. "Stupid Name.")))))
  ?proto :binary :compact :json :tuple)
