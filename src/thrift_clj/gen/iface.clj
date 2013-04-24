(ns ^{ :doc "Iface Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.gen.iface
  (:require [thrift-clj.gen.core :as c]
            [thrift-clj.thrift.services :as s]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp]))

;; ## Iface
;;
;; The Service Interface is located at `<Service>$Iface`. It can be
;; accessed by creating a namespace that contains a single function
;; for each interface method, transparently converting between 
;; Clojure and Thrift representations.

(nsp/def-reload-indicator reload-iface?)

(defn generate-thrift-iface-import
  "Generate single Iface import."
  [service-class iface-alias]
  (let [cls (u/full-class-symbol service-class)
        iface-cls (u/inner cls "Iface")
        mth (s/thrift-service-methods service-class)
        param-syms (repeatedly gensym)
        iface-sym (vary-meta (gensym "iface-") assoc :tag iface-cls)]
    `(do 
       ~@(when (reload-iface? iface-cls)
           [`(nsp/internal-ns-remove '~iface-cls)])
       (nsp/internal-ns 
         ~iface-cls
         ~@(for [{:keys[name params]} mth]
             (let [params (take (count params) param-syms)]
               `(defn ~(symbol name)
                  [~iface-sym ~@params]
                  (c/->clj
                    (. ~iface-sym
                       ~(symbol name) 
                       ~@(map #(list `c/->thrift %) params)))))))
       (nsp/internal-ns-require '~iface-cls '~iface-alias))))

(defn generate-thrift-iface-imports
  "Generate Iface Namespace for the given Service-Class/Iface-Alias pairs."
  [service-map]
  (for [[service-class iface-alias] service-map]
    (let [iface-alias (or iface-alias (u/class-symbol service-class))]
      (generate-thrift-iface-import service-class iface-alias))))
