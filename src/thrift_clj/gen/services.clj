(ns ^{ :doc "Thrift Service Import"
       :author "Yannick Scherer" }
  thrift-clj.gen.services
  (:require [thrift-clj.gen.iface :as ifc]
            [thrift-clj.gen.types :as t]
            [thrift-clj.thrift.services :as s]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp])
  (:use clojure.tools.logging)
  (:import (org.apache.thrift.protocol TBinaryProtocol)))

;; ## Services
;;
;; Services need an alias that will be used as a var to contain a reference to the service
;; class. Also, the service interface will be imported using that alias.

;; ## Service Implementation

(defmulti map->iface
  "Multimethod that can create a the service's inner `Iface` by dispatching
   a map of method-keyword/function pairs using a class."
  (fn [cls m] cls))

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

(defn- generate-proxy-fn
  "Generate Function that creates a Proxy around the given class' `Iface` based on
   a map of method-name/method-fn pairs, transparently converting between Clojure
   and Thrift representations."
  [proxy-fn-sym cls mth]
  (let [iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        handler (gensym "handler-")]
    `(defn- ~proxy-fn-sym
       [~handler]
       (proxy [~iface] []
         ~@(for [{:keys[name params]} mth]
             (let [params (take (count params) param-syms)
                   method-name (str cls "." name)]
               `(~(symbol name) [~@params]
                   (debug ~(str "[" method-name "]") "Entering Method ...")
                   (if-let [h# (get ~handler ~(keyword name))]
                     (let [r# (t/->thrift (h# ~@(map #(list `t/->clj %) params)))]
                       (debug  ~(str "[" method-name "]") "Done.")
                       r#)
                     (throw (Exception. ~(str "[Thrift] Service Method not implemented: " method-name)))))))))))

(defn- generate-service-defmethods
  "Generate \"Hooks\" to be called by `service.`"
  [proxy-fn-sym cls]
  (let [iface (u/inner cls "Iface")
        proc (u/inner cls "Processor")]
    `(do 
       (defmethod map->iface ~cls
         [~'_ m#]
         (~proxy-fn-sym m#))
       (defmethod iface->processor ~iface
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
              proxy-sym (gensym)]
          `(do
             ~@(when (reload-service? cls)
                 [`(nsp/internal-ns-remove '~cls)])
             (nsp/internal-ns
               ~cls
               ~(generate-proxy-fn proxy-sym cls mth)
               ~(generate-service-defmethods proxy-sym cls))
             ~(ifc/generate-thrift-iface-import service-class service-alias)
             (def ~service-alias ~cls)
             true))
        (catch Exception ex 
          (throw (Exception. (str "Failed to import Service: " cls) ex)))))))
