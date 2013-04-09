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
;;
;; For each Thrift type, we will create a Clojure type of the same name.
;; The Thrift type will be extended to implement conversion to Clojure,
;; whilst the Clojure type will be extended to implement the opposite 
;; direction.
;;
;; __NOTE:__ We do not have to use `import` because Google Reflections will already
;; have loaded the class.

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
