(ns ^{ :doc "Testing custom exception handling."
       :author "Yannick Scherer" }
  thrift-clj.exception-test
  (:require [thrift-clj.core :as thrift])
  (:use midje.sweet))

(thrift/import-types [thriftclj.exceptions StorageError])

(fact "about trying to throw generated Clojure types"
  (throw (StorageError. "No reason.")) => (throws ClassCastException))

(fact "about throwing Thrift exception"
  (throw (doto (thriftclj.exceptions.StorageError.)
           (.setReason "No reason.")))
    => (throws thriftclj.exceptions.StorageError))

(fact "about thrift/throw"
  (thrift/throw (StorageError. "No reason."))
    => (throws thriftclj.exceptions.StorageError))

(fact "about thrift/try"
  (thrift/try
    (do
      (throw (Exception. "Some Exception."))
      ::some)
    (catch Exception ex
      ::error)) => ::error

  (thrift/try
    (do
      (thrift/throw (Exception. "Some Exception."))
      ::some)
    (catch Exception ex
      ::error)) => ::error

  (thrift/try
    (do
      (thrift/throw (thriftclj.exceptions.StorageError.))
      ::some)
    (catch thriftclj.exceptions.StorageError ex
      ::storage-error-thrift)
    (catch StorageError ex
      ::storage-error)
    (catch Exception ex
      ::error)) => ::storage-error-thrift

  (thrift/try
    (do
      (thrift/throw (thriftclj.exceptions.StorageError.))
      ::some)
    (catch StorageError ex
      ::storage-error)
    (catch Exception ex
      ::error)) => ::storage-error

  (thrift/try
    (do
      (thrift/throw (StorageError. "No reason."))
      ::some)
    (catch StorageError ex
      ::storage-error)
    (catch Exception ex
      ::error)) => ::storage-error)
