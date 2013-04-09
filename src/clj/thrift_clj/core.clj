(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [load])
  (:use thrift-clj.core.thrift-types))

;; ## Main Macro

(defmacro load
  "Load all Thrift Entities in the given packages."
  [& packages]
  (let [packages (map str packages)
        type-definitions (generate-thrift-types packages)]
    `(do
       ~@type-definitions)))
