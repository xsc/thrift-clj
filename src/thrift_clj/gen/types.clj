(ns ^{ :doc "Thrift Type Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.types
  (:use thrift-clj.gen.core
        clojure.tools.logging)
  (:require [thrift-clj.thrift.types :as t]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp]))

;; ## Types
;;
;; A Thrift type/Clojure record pair will be created, both being convertable
;; between these representations.
;;
;; Each Thrift type to be imported will get its own internal namespace
;; (for reusability) but will be injected into the calling namespace
;; using `import`.

;; ## Conversion

(defmacro ^:private defbase
  "Define types that are identical in Thrift and Clojure."
  [t]
  `(extend-type ~t 
     Value
     (->thrift* [v#] v#)
     (->clj* [v#] v#)))

(defbase nil)
(defbase java.lang.String)
(defbase java.lang.Byte)
(defbase java.lang.Integer)
(defbase java.lang.Long)
(defbase java.lang.Double)
(defbase java.lang.Boolean)

(extend-type java.util.Set
  Value
  (->thrift* [this]
    (java.util.HashSet. (map ->thrift this)))
  (->clj* [this]
    (set (map ->clj this))))

(extend-type java.util.List
  Value
  (->thrift* [this]
    (java.util.ArrayList. (map ->thrift this)))
  (->clj* [this]
    (vec (map ->clj this))))

(extend-type java.util.Map
  Value
  (->thrift* [this]
    (java.util.HashMap. 
      (->> this
        (map 
          (fn [[k v]]
            (vector (->thrift k) (->thrift v))))
        (into {}))))
  (->clj* [this]
    (->> this
      (map 
        (fn [[k v]]
          (vector (->clj k) (->clj v))))
      (into {}))))

(extend-type clojure.lang.IPersistentVector
  Value
  (->thrift* [this]
    (java.util.ArrayList. (map ->thrift this)))
  (->clj* [this]
    (vec (map ->clj this))))

;; ## Form Generation Helpers

(defn- extend-thrift-type
  "Make Thrift Type implement the `Value` protocol."
  [clojure-type thrift-type mta]
  (let [v (gensym "v-")
        find-fn (u/static (u/inner thrift-type "_Fields") "findByThriftId")]
    `(extend-type ~thrift-type
       Value
       (->thrift* [~v] ~v)
       (->clj* [~v]
         (new 
           ~clojure-type
           ~@(for [id (map :id mta)]
               `(->clj (.getFieldValue ~v (~find-fn ~id)))))))))

(defn- generate-clojure-type
  "Generate Record Type that can be converted to its Thrift equivalent."
  [clojure-type thrift-type mta]
  (let [fields (map (comp symbol :name) mta)
        find-fn (u/static (u/inner thrift-type "_Fields") "findByThriftId")
        obj (gensym "o")]
    `(defrecord ~clojure-type [~@fields]
       Value
       (->clj* [v#] v#)
       (->thrift* [~'_]
         (let [~obj (new ~thrift-type)]
           ~@(for [[{:keys[require id wrapper]} sym] (map vector mta fields)]
               (let [value (if wrapper `(~wrapper ~sym) sym)]
                 (if (= require :optional)
                   `(.setFieldValue ~obj (~find-fn ~id) (and ~sym (->thrift ~value)))
                   `(if-not ~sym
                      (throw (Exception. ~(str "Not an optional field: " sym)))
                      (.setFieldValue ~obj (~find-fn ~id) (->thrift ~value))))))
           ~obj)))))

;; ## Import

(nsp/def-reload-indicator reload-types?)

(defn- generate-struct-import
  "Import a Struct Type. This creates a Clojure type and extends the Thrift type to be
   convertable to Clojure (and vice versa)."
  [t]
  (let [current-ns (ns-name *ns*)
        clojure-type (u/class-symbol t)
        thrift-type (u/full-class-symbol t)]
    (try
      (when-let [mta (t/type-metadata t)]
        `(do
           ~@(when (reload-types? thrift-type)
               [`(ns-unmap '~current-ns '~clojure-type)
                `(nsp/internal-ns-remove '~thrift-type)])
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
                 ex))))))

(defn- generate-enum-import
  "Import an Enum Type. This equals a normal import."
  [t]
  (let [current-ns (ns-name *ns*)
        thrift-type (u/full-class-symbol t)]
    `(do
       ~(when (reload-types? thrift-type)
          `(ns-unmap '~current-ns '~thrift-type))
       (import '~thrift-type))))

(defn generate-thrift-type-imports
  "Generate a Clojure Type that corresponds to a given Thrift Type for a seq
   of Thrift type classes."
  [types]
  (for [t types]
    (cond (t/thrift-struct? t) (generate-struct-import t)
          (t/thrift-enum? t) (generate-enum-import t)
          :else (throw (Exception. (str "Not a Thrift type/enum: " t))))))
