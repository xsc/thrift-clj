(ns ^{ :doc "Tests for Thrift Client Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.clients-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet)
  (:import org.apache.thrift.transport.TTransportException))

;; ## Import

(thrift/import-clients
  [thriftclj.services.TelephoneBook :as TB])

(fact "about the imported service interface"
  TB/storePerson => fn?
  TB/findByName => fn?
  TB/findByLocation => fn?)

(fact "about current namespace after service import"
  storePerson => nil?
  findByName => nil?
  findByLocation => nil?)

(fact "about the client var"
  TB => thriftclj.services.TelephoneBook$Client)

(fact "about unsuccessful TCP connect"
  (thrift/connect! TB ["localhost" (+ 40000 (rand-int 10000))])
    => (throws TTransportException
               (fn [^TTransportException ex]
                 (and
                   (= (class (.getCause ex)) java.net.ConnectException)
                   (= (.getType ex) TTransportException/NOT_OPEN)))))
