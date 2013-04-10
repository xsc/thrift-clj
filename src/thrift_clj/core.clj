(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [load])
  (:use [potemkin :only [import-macro import-fn]])
  (:require [thrift-clj.core.thrift-types :as t]
            [thrift-clj.core.thrift-services :as s]
            [thrift-clj.core.thrift-clients :as c]
            [thrift-clj.server :as srv]))

;; ## Imported

(import-macro s/defservice)

(import-macro c/create-client)
(import-macro c/with-client)
(import-fn c/start-client!)
(import-fn c/stop-client!)

(import-fn t/clj->thrift)
(import-fn t/thrift->clj)

(import-fn srv/start-server!)
(import-fn srv/stop-server!)

;; ## Main Macros

(defmacro load-types
  "Load Thrift Types from the given packages."
  [& packages]
  (let [packages (map str packages)]
    `(do ~@(t/generate-thrift-types packages) true)))

(defmacro load-services
  "Load Thrift Services from the given packages."
  [& packages]
  (let [packages (map str packages)]
    `(do ~@(s/generate-thrift-services packages) true)))

(defmacro load
  "Load all Thrift Entities in the given packages."
  [& packages]
  `(do
     (load-types ~@packages)
     (load-services ~@packages)))
