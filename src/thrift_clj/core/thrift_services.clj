(ns ^{ :doc "Thrift Service Import"
       :author "Yannick Scherer" }
  thrift-clj.core.thrift-services
  (:require [thrift-clj.utils :as u]
            [thrift-clj.thrift :as thrift]
            [thrift-clj.reflect :as reflect])
  (:use thrift-clj.core.thrift-types
        thrift-clj.core.thrift-clients
        clojure.tools.logging)
  (:import (org.apache.thrift.protocol TBinaryProtocol)))

;; ## Services

(defmulti map->iface
  "Multimethod that can create a the service's inner `Iface` by dispatching
   a map of method-keyword/function pairs using a class."
  (fn [cls m] cls))

(defmulti iface->processor
  "Multimethod that converts an Iface to a Processor."
  class)

(defmacro defservice
  "Implement the given Service (a fully qualified class name)."
  [id service-cls & implementation]
  (let [m (reduce
            (fn [m [id bindings & method-impl]]
              (assoc m (keyword id) 
                     `(fn [~@bindings] ~@method-impl)))
            {} implementation)]
    `(def ~id
       (map->iface ~service-cls ~m))))

;; ## Form Generation

(defn- generate-thrift-iface
  "Make a given Service accessible by `defservice` by implementing the multimethod
   `map->iface` for the fully qualified class name given."
  [n cls mth]
  (let [proc (u/inner cls "Processor")
        iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        handler (gensym "handler-")
        conv-sym (gensym)]
    `(do
       (defn- ~conv-sym
         [~handler]
         (proxy [~iface] []
           ~@(for [{:keys[name params]} mth]
               (let [params (take (count params) param-syms)]
                 `(~(symbol name) [~@params]
                     (debug ~(str "[" n "." name "]") "Entering Method ...")
                     (if-let [h# (get ~handler ~(keyword name))]
                       (let [r# (->thrift (h# ~@(map #(list `->clj %) params)))]
                         (debug  ~(str "[" n "." name "]") "Done.")
                         r#)
                       (throw (Exception. ~(str "[Thrift] Service Method not implemented: " n "." name)))))))))
       (defmethod map->iface ~cls
         [~'_ m#]
         (~conv-sym m#))
       (defmethod iface->processor ~iface
         [this#]
         (new ~proc this#))
       (def ~n ~cls))))

(defn import-thrift-services
  "Import Thrift services given as a map of service-class/service-name pairs."
  [service-map]
  (for [[s n] service-map]
    (let [n (or n (u/class-symbol s))
          cls (u/full-class-symbol s)
          mth (reflect/thrift-service-methods s)]
      (generate-thrift-iface n cls mth))))
