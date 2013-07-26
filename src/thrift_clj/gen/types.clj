(ns ^{ :doc "Thrift Type Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.types
  (:use [clojure.tools.logging :only [debug info warn error]])
  (:require [thrift-clj.gen.core :as c]
            [thrift-clj.thrift.types :as t]
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
     c/Value
     (to-thrift* [v#] v#)
     (to-thrift-unchecked* [v#] v#)
     (to-clj* [v#] v#)))

(defbase nil)
(defbase java.lang.String)
(defbase java.lang.Byte)
(defbase java.lang.Integer)
(defbase java.lang.Long)
(defbase java.lang.Double)
(defbase java.lang.Boolean)

(extend-type java.util.Set
  c/Value
  (to-thrift* [this]
    (let [^java.util.List l (map c/->thrift this)]
      (java.util.HashSet. l)))
  (to-thrift-unchecked* [this]
    (c/to-thrift* this))
  (to-clj* [this]
    (set (map c/->clj this))))

(extend-type java.util.List
  c/Value
  (to-thrift* [this]
    (map c/->thrift this))
  (to-thrift-unchecked* [this]
    (c/to-thrift* this))
  (to-clj* [this]
    (vec (map c/->clj this))))

(extend-type java.util.Map
  c/Value
  (to-thrift* [this]
    (->> this
      (map 
        (fn [[k v]]
          (vector (c/->thrift k) (c/->thrift v))))
      (into {})))
  (to-thrift-unchecked* [this]
    (c/to-thrift* this))
  (to-clj* [this]
    (->> this
      (map 
        (fn [[k v]]
          (vector (c/->clj k) (c/->clj v))))
      (into {}))))

(extend-type clojure.lang.IPersistentVector
  c/Value
  (to-thrift* [this]
    (map c/->thrift this))
  (to-thrift-unchecked* [this]
    (c/to-thrift* this))
  (to-clj* [this]
    (vec (map c/->clj this))))

;; ## Form Generation Helpers

(defn- extend-thrift-type
  "Make Thrift Type implement the `Value` protocol."
  [clojure-type thrift-type mta]
  (let [v (gensym "v-")
        find-fn (u/static (u/inner thrift-type "_Fields") "findByThriftId")]
    `(extend-type ~thrift-type
       c/Value
       (to-thrift* [~v] (new ~thrift-type ~v))
       (to-thrift-unchecked* [~v] (new ~thrift-type ~v))
       (to-clj* [~v]
         (new 
           ~clojure-type
           ~@(for [id (map :id mta)]
               `(c/->clj (.getFieldValue ~v (~find-fn ~id)))))))))

(defn- generate-clojure-type
  "Generate Record Type that can be converted to its Thrift equivalent."
  [clojure-type thrift-type mta]
  (let [fields (map (comp symbol :name) mta)
        find-fn (u/static (u/inner thrift-type "_Fields") "findByThriftId")
        obj (gensym "o")]
    `(defrecord ~clojure-type [~@fields]
       c/Value
       (to-clj* [v#] v#)
       (to-thrift-unchecked* [~'_]
         (let [~obj (new ~thrift-type)]
           ~@(for [[{:keys[require id wrapper]} sym] (map vector mta fields)]
               (let [value (if wrapper `(~wrapper ~sym) sym)]
                 `(.setFieldValue ~obj (~find-fn ~id) (and ~sym (c/->thrift ~value)))))
           ~obj))
       (to-thrift* [~'_]
         (let [~obj (new ~thrift-type)]
           ~@(for [[{:keys[require id wrapper]} sym] (map vector mta fields)]
               (let [value (if wrapper `(~wrapper ~sym) sym)]
                 (if (= require :optional)
                   `(.setFieldValue ~obj (~find-fn ~id) (and ~sym (c/->thrift ~value)))
                   `(if (nil? ~sym)
                      (throw (Exception. ~(str "Not an optional field: " sym)))
                      (.setFieldValue ~obj (~find-fn ~id) (c/->thrift ~value))))))
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
      (when-let [mta (t/thrift-type-metadata t)]
        `(do
           ~@(when (reload-types? thrift-type)
               [`(ns-unmap '~current-ns '~clojure-type)
                `(ns-unmap '~current-ns '~(symbol (str "->" clojure-type)))
                `(ns-unmap '~current-ns '~(symbol (str "map->" clojure-type)))
                `(nsp/internal-ns-remove '~thrift-type)])
           (nsp/internal-ns
             ~thrift-type 
             ~(generate-clojure-type clojure-type thrift-type mta)
             ~(extend-thrift-type clojure-type thrift-type mta))
           (nsp/internal-ns-import
             ~thrift-type
             ~clojure-type)
           (nsp/internal-ns-refer
             '~thrift-type
             '~(symbol (str "->" clojure-type))
             '~(symbol (str "map->" clojure-type)))))
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
