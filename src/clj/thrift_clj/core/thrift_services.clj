(ns ^{ :doc "Thrift Service Import"
       :author "Yannick Scherer" }
  thrift-clj.core.thrift-services
  (:require [thrift-clj.reflect :as reflect]
            [thrift-clj.thrift :as thrift]
            [thrift-clj.utils :as u])
  (:use thrift-clj.core.thrift-types
        clojure.tools.logging))

;; ## Services
;;
;; Services are imported by creating wrapper functions that build the
;; Client/Server objects, as well as TODO

(defmulti map->processor
  "Multimethod that can create an `org.apache.thrift.TProcessor` by dispatching
   a map of method-keyword/function pairs using a fully qualified class name."
  (fn [cls m] cls))

(defmacro defservice
  "Implement the given Service (a fully qualified class name)."
  [id service-cls & implementation]
  (let [m (reduce
            (fn [m [id bindings & method-impl]]
              (assoc m (keyword id) 
                     `(fn [~@bindings] ~@method-impl)))
            {} implementation)]
    `(def ~id
       (map->processor '~service-cls ~m))))

;; ## Form Generation

(defn- generate-thrift-processor
  "Make a given Service accessible by `defservice` by implementing the multimethod
   `map->processor` for the fully qualified class name given."
  [n cls mth]
  (let [proc (u/inner cls "Processor")
        iface (u/inner cls "Iface")
        param-syms (repeatedly gensym)
        handler (gensym "handler-")
        conv-sym (gensym)]
    `(do
       (defn- ~conv-sym
         [~handler]
         (new
           ~proc
           (proxy [~iface] []
             ~@(for [{:keys[name params]} mth]
                 (let [params (take (count params) param-syms)]
                   `(~(symbol name) [~@params]
                       (debug ~(str "[" n "." name "]") "Entering Method ...")
                       (if-let [h# (get ~handler ~(keyword name))]
                         (let [r# (clj->thrift (h# ~@(map #(list `thrift->clj %) params)))]
                           (debug  ~(str "[" n "." name "]") "Done.")
                           r#)
                         (throw (Exception. ~(str "[Thrift] Service Method not implemented: " n "." name))))))))))
       (defmethod map->processor '~cls
         [~'_ m#]
         (~conv-sym m#)))))

(defn generate-thrift-services
  "Generate everything needed for accessing Thrift Services."
  [packages]
  (let [services (reflect/thrift-services packages)]
    (for [s services]
      (let [n (reflect/class-symbol s)
            cls (reflect/full-class-symbol s)
            mth (reflect/thrift-service-methods s)]
        `(do
           ~(generate-thrift-processor n cls mth))))))
