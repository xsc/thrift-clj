(ns ^{ :doc "Thrift Type Import"
       :author "Yannick Scherer" }
  thrift-clj.core.thrift-types
  (:use [potemkin.types :only [defrecord+]]
        clojure.tools.logging)
  (:require [thrift-clj.thrift :as thrift]
            [thrift-clj.utils :as u]))

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

;; ### Protocol

(defprotocol ClojureType
  "Protocol for Clojure Values that can be converted to a Thrift equivalent."
  (clj->thrift* [this]))

(defprotocol ThriftType
  "Protocol for Thrift Values that can be converted to a Clojure equivalent."
  (thrift->clj* [this]))

(defn ->thrift
  "Convert Clojure Type to Thrift Type if possible."
  [v]
  (if (satisfies? ClojureType v)
    (clj->thrift* v)
    v))

(defn ->clj
  "Convert Thrift Type to Clojure Type if possible."
  [v]
  (if (satisfies? ThriftType v)
    (thrift->clj* v)
    v))

;; ### Form Generation

(defn- generate-clojure-type
  "Generate Record Type that can be converted to its Thrift equivalent."
  [n cls mta]
  (let [fields (map (comp symbol :name) mta)]
    `(defrecord+ ~n [~@fields]
       ClojureType
       (clj->thrift* [~'_]
         (doto (new ~cls)
           ~@(for [[field sym] (map vector mta fields)]
               (let [v (if-let [w (:wrapper field)]
                         `(~w ~sym)
                         sym)]
                 `(.setFieldValue
                    (~(u/static (u/inner cls "_Fields") "findByThriftId") ~(:id field))
                    (->thrift ~v)))))))))

(defn- extend-thrift-type
  "Let the given Thrift Type implement the protocol `ThriftType`, making it
   convertable to its Clojure equivalent."
  [n cls mta]
  (let [this-sym (gensym "this-")]
    `(extend-type ~cls
       ThriftType
       (thrift->clj* [~this-sym]
         (new ~n 
              ~@(for [field mta]
                  `(->clj
                     (.getFieldValue 
                       ~this-sym 
                       (~(u/static (u/inner cls "_Fields") "findByThriftId") ~(:id field))))))))))

(defn import-thrift-types
  "Generate a Clojure Type that corresponds to a given Thrift Type for
   a map of type-class/type-name pairs."
  [type-map]
  (for [[t n] type-map]
    (let [n (or n (u/class-symbol t))
          cls (u/full-class-symbol t)]
      (try
        (when-let [mta (thrift/type-metadata t)]
          `(do
             ~(generate-clojure-type n cls mta)
             ~(extend-thrift-type n cls mta)
             true))
        (catch Exception ex
          (error ex "when importing type:" cls)
          (throw (Exception.
                   (str "Error when importing `" cls "': " (.getMessage ex))
                   ex)))))))
