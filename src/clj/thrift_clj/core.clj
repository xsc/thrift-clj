(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [load])
  (:require [thrift-clj.reflect :as reflect]
            [thrift-clj.thrift :as thrift]))

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
;; For each Thrift type, we will create a Clojure type of the same name,
;; with the same fields.
;; The Thrift type will be extended to implement conversion to Clojure,
;; whilst the Clojure type will be extended to implement the opposite 
;; direction.
;;
;; __NOTE:__ We do not have to use `import` because Google Reflections will already
;; have loaded the class.

(defprotocol ClojureType
  "Protocol for Clojure Values that can be converted to a Thrift equivalent."
  (clj->thrift [this]))

(defprotocol ThriftType
  "Protocol for Thrift Values that can be converted to a Clojure equivalent."
  (thrift->clj [this]))

(defn- generate-clojure-type
  "Generate Record Type that can be converted to its Thrift equivalent."
  [n cls mta]
  (let [fields (map (comp symbol :name) mta)]
    `(defrecord ~n [~@fields]
       ClojureType
       (clj->thrift [~'_]
         (doto (new ~cls)
           ~@(for [[field sym] (map vector mta fields)]
               `(.setFieldValue
                  (~(symbol (str cls "$_Fields/findByThriftId")) ~(:id field))
                  ~sym)))))))

(defn- extend-thrift-type
  "Let the given Thrift Type implement the protocol `ThriftType`, making it
   convertable to its Clojure equivalent."
  [n cls mta]
  (let [this-sym (gensym "this-")]
    `(extend-type ~cls
       ThriftType
       (thrift->clj [~this-sym]
         (new ~n 
              ~@(for [field mta]
                  `(.getFieldValue 
                     ~this-sym 
                     (~(symbol (str cls "$_Fields/findByThriftId")) ~(:id field)))))))))

(defn- generate-thrift-types
  "Generate a Clojure Type that corresponds to a given Thrift Type."
  [packages]
  (let [types (reflect/thrift-types packages)]
    (for [t types]
      (let [n (reflect/class-symbol t)
            cls (reflect/full-class-symbol t)
            mta (thrift/type-metadata t)]
        `(do
           ~(generate-clojure-type n cls mta)
           ~(extend-thrift-type n cls mta)
           true)))))
