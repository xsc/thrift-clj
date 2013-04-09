(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [load])
  (:require [thrift-clj.reflect :as reflect]))

;; ## Main Macro

(declare generate-thrift-types
         import-thrift-type)

(defmacro load
  "Load all Thrift Entities in the given packages."
  [& packages]
  (let [packages (map str packages)
        type-definitions (generate-thrift-types packages)]
    `(do
       ~@type-definitions)))

;; ## Types

(defn- generate-thrift-constructor
  [t n cls-symbol constructors]
  `(defn ~n
     ~(str "Wrapper around `" cls-symbol "`.")
     ~@(->>
         (for [c constructors]
           (when-not (and (= (count c) 1) (= (first c) t))
             (let [args (for [_ c] (gensym))]
               `([~@args] (new ~cls-symbol ~@args)))))
         (filter (complement nil?)))))

(defn- generate-thrift-types
  [packages]
  (let [types (reflect/thrift-types packages)]
    (for [t types]
      (let [n (reflect/class-symbol t)
            cls (reflect/full-class-symbol t)
            constructors (reflect/class-constructors t)]
        (generate-thrift-constructor t n cls constructors)))))
