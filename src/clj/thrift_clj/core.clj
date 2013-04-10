(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [load])
  (:use [potemkin :only [import-macro import-fn]])
  (:require [thrift-clj.core.thrift-types :as t]
            [thrift-clj.core.thrift-services :as s]))

;; ## Imported

(import-macro s/defservice)
(import-fn t/clj->thrift)
(import-fn t/thrift->clj)

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
