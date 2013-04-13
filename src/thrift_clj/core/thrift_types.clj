(ns ^{ :doc "Thrift Type Import"
       :author "Yannick Scherer" }
  thrift-clj.core.thrift-types
  (:use [potemkin.types :only [defrecord+]]
        clojure.tools.logging)
  (:require [thrift-clj.thrift.types :as t]
            [thrift-clj.utils :as u]
            [thrift-clj.utils.namespace :as nsp]))

;; ## Concept
;;
;; A Thrift type/Clojure record pair will be created, both being convertable
;; between these representations.
;;
;; Each Thrift type to be imported will get its own internal namespace
;; (for reusability) but will be injected into the calling namespace
;; using `import`.

;; ## Conversion

(defprotocol Value
  "Protocol for Values."
  (->thrift [this]
    "Convert Value to Thrift Representation if possible.")
  (->clj [this]
    "Convert Value to Clojure Representation if possible."))

(defmacro ^:private defbase
  "Define types that are identical in Thrift and Clojure."
  [t]
  `(extend-type ~t 
     Value
     (->thrift [v#] v#)
     (->clj [v#] v#)))

(defbase java.lang.String)
(defbase java.lang.Byte)
(defbase java.lang.Integer)
(defbase java.lang.Long)
(defbase java.lang.Double)
(defbase java.lang.Boolean)

;; ## Form Generation Helpers

(defn- extend-thrift-type
  "Make Thrift Type implement the `Value` protocol."
  [clojure-type thrift-type mta]
  (let [v (gensym "v-")
        find-fn (u/static (u/inner thrift-type "_Fields") "findByThriftId")]
    `(extend-type ~thrift-type
       Value
       (->thrift [~v] ~v)
       (->clj [~v]
         (new 
           ~clojure-type
           ~@(for [id (map :id mta)]
               `(->clj (.getFieldValue ~v (~find-fn ~id)))))))))

(defn- generate-clojure-type
  "Generate Record Type that can be converted to its Thrift equivalent."
  [clojure-type thrift-type mta]
  (let [fields (map (comp symbol :name) mta)
        find-fn (u/static (u/inner thrift-type "_Fields") "findByThriftId")]
    `(defrecord ~clojure-type [~@fields]
       Value
       (->clj [v#] v#)
       (->thrift [~'_]
         (doto (new ~thrift-type)
           ~@(for [[field sym] (map vector mta fields)]
               (let [v (if-let [w (:wrapper field)]
                         `(~w ~sym)
                         sym)]
                 `(.setFieldValue (~find-fn ~(:id field)) (->thrift ~v)))))))))

;; ## Import

(defn import-thrift-types
  "Generate a Clojure Type that corresponds to a given Thrift Type for a seq
   of Thrift type classes."
  [types]
  (for [t types]
    (let [clojure-type (u/class-symbol t)
          thrift-type (u/full-class-symbol t)]
      (try
        (when-let [mta (t/type-metadata t)]
          `(do
             (nsp/internal-ns
               ~thrift-type 
               ~(generate-clojure-type clojure-type thrift-type mta)
               ~(extend-thrift-type clojure-type thrift-type mta))
             (nsp/internal-ns-import
               ~thrift-type
               ~clojure-type)))
        (catch Exception ex
          (error ex "when importing type:" thrift-type)
          (throw (Exception.
                   (str "Error when importing `" thrift-type "': " (.getMessage ex))
                   ex)))))))
