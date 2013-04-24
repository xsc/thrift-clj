(ns ^{ :doc "Thrift Service Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.services
  (:use [thrift-clj.gen.core :only [->clj ->thrift ->thrift-unchecked]])
  (:require [thrift-clj.gen.iface :as ifc]
            [thrift-clj.thrift.services :as s]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp])
  (:use [clojure.tools.logging :only [debug info warn error]])
  (:import (org.apache.thrift.protocol TBinaryProtocol)))

;; ## Services
;;
;; Services need an alias that will be used as a var to contain a reference to the service
;; class. Also, the service interface will be imported using that alias.

;; ## Service Implementation

(defmulti map->iface
  "Multimethod that can create a the service's inner `Iface` by dispatching
   a map of method-keyword/function pairs using a class."
  (fn [cls _] cls))

(defmulti iface->processor
  "Multimethod that converts an Iface to a Processor."
  class)

(defmacro service
  "Implement the given, previously imported Service."
  [service-cls & implementation]
  (let [[options implementation] (loop [i implementation
                                        o []]
                                   (cond (not (seq i)) [o i]
                                         (keyword? (first i)) (recur (drop 2 i) (concat o (take 2 i)))
                                         :else [o i]))
        options (apply hash-map options)
        m (reduce
            (fn [m [id bindings & method-impl]]
              (assoc m (keyword id) 
                     `(fn [~@bindings] ~@method-impl)))
            {} implementation)]
    `(let [~@(:let options)]
       (map->iface ~service-cls ~m))))

(defmacro defservice
  "Define var containing the service."
  [id service-cls & implementation]
  `(def ~id (service ~service-cls ~@implementation)))

(defmacro defservice-fn
  "Create function for service generation."
  [id service-cls bindings & implementation]
  `(defn ~id 
     [~@bindings]
     (service ~service-cls ~@implementation)))

;; ## Form Generation Helpers

(defn- generate-iface-type
  "Generate Type that implements a services Iface, delegating method calls to
   the different handler functions it consists of."
  [type-sym cls mth]
  (let [iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        handler-syms (repeatedly #(gensym "handler-"))]
    `(deftype ~type-sym [~@(take (count mth) handler-syms)]
       ~iface
       ~@(for [[{:keys[name params]} handler] (map vector mth handler-syms)]
           (let [params (take (count params) param-syms)
                 method-name (str cls "." name)
                 impl-sym (symbol name)
                 handler-call `(->thrift (~handler ~@(map #(list `->clj %) params)))]
             `(~impl-sym [this# ~@params]
                (debug ~(str "[" method-name "]") "Entering Method ...")
                (when-not ~handler
                  (throw (Exception. ~(str "[" method-name "] Service Method not implemented."))))
                (let [r# ~handler-call]
                  (debug ~(str "[" method-name "] Done."))
                  r#)))))))

(defn- generate-service-defmethods
  "Generate \"Hooks\" to be called by `service.`"
  [type-sym cls mth]
  (let [proc (u/inner cls "Processor")
        m (gensym "m")]
    `(do 
       (defmethod map->iface ~cls
         [~'_ ~m]
         (new ~type-sym 
              ~@(for [{:keys[name params]} mth]
                  `(~(keyword name) ~m))))
       (defmethod iface->processor ~type-sym
         [this#]
         (new ~proc this#)))))

;; ## Import

(nsp/def-reload-indicator reload-service?)

(defn generate-thrift-service-imports
  "Import Thrift services given as a map of service-class/service-name pairs."
  [service-map]
  (for [[service-class service-alias] service-map]
    (let [cls (u/full-class-symbol service-class)]
      (try
        (let [service-alias (or service-alias (u/class-symbol service-class))
              mth (s/thrift-service-methods service-class)
              type-sym (gensym (str (u/class-symbol service-class)))]
          `(do
             ~@(when (reload-service? cls)
                 [`(nsp/internal-ns-remove '~cls)])
             (nsp/internal-ns
               ~cls
               ~(generate-iface-type type-sym cls mth)
               ~(generate-service-defmethods type-sym cls mth))
             ~(ifc/generate-thrift-iface-import service-class service-alias)
             (def ~service-alias ~cls)
             true))
        (catch Exception ex 
          (.printStackTrace ex)
          (throw (Exception. (str "Failed to import Service: " cls) ex)))))))
