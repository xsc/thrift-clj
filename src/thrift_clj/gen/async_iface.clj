(ns ^{ :doc "Asynchronous Iface Import."
       :author "Yannick Scherer" }
  thrift-clj.gen.async-iface
  (:require [thrift-clj.gen.core :as c]
            [thrift-clj.thrift.services :as s]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp])
  (:import [org.apache.thrift.async AsyncMethodCallback TAsyncMethodCall]))

;; Imported like the normal Iface, but with callbacks as last parameters, e.g.
;;
;;    (S/doSomething a b c :on-error ... :on-complete ...)
;;

(nsp/def-reload-indicator reload-iface?)

(defn generate-callback
  "Generate AsyncMethodCallback object encapsulating result and error handlers."
  [call-cls on-complete on-error]
  (let [response (vary-meta (gensym "r") assoc :tag call-cls)]
    `(reify AsyncMethodCallback
       (onComplete [this# r#]
         (let [~response r#
               result# (.getResult ~response)]
           (~on-complete (c/->clj result#))))
       (onError [this# exception#]
         (~on-error (c/->clj exception#))))))

(defn generate-thrift-async-iface-import
  "Generate single Iface import."
  [service-class iface-alias]
  (let [cls (u/full-class-symbol service-class)
        iface-cls (u/inner cls "AsyncIface")
        client-cls (u/inner cls "AsyncClient")
        on-error (gensym "on-error")
        on-complete (gensym "on-complete")
        param-syms (repeatedly gensym)
        iface-sym (vary-meta (gensym "iface-") assoc :tag iface-cls)
        mth (s/thrift-service-methods service-class)]
    `(do 
       ~@(when (reload-iface? iface-cls)
           [`(nsp/internal-ns-remove '~iface-cls)])
       (nsp/internal-ns 
         ~iface-cls
         ~@(for [{:keys[name params]} mth]
             (let [params (take (count params) param-syms)
                   call-cls (u/inner client-cls (str name "_call"))]
               `(defn ~(symbol name)
                  [~iface-sym ~@params & {:keys [] :as callbacks#}]
                  (let [~on-error (:on-error callbacks# (constantly nil))
                        ~on-complete (:on-complete callbacks# (constantly nil))
                        handler# ~(generate-callback call-cls on-complete on-error)]
                    (. ~iface-sym
                       ~(symbol name) 
                       ~@(map #(list `c/->thrift %) params)
                       handler#))))))
       (nsp/internal-ns-require '~iface-cls '~iface-alias))))

(defn generate-thrift-async-iface-imports
  "Generate Iface Namespace for the given Service-Class/Iface-Alias pairs."
  [service-map]
  (for [[service-class iface-alias] service-map]
    (let [iface-alias (or iface-alias (u/class-symbol service-class))]
      (generate-thrift-async-iface-import service-class iface-alias))))
