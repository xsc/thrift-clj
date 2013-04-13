(ns ^{ :doc "Iface Wrappers"
       :author "Yannick Scherer" }
  thrift-clj.gen.iface
  (:require [thrift-clj.gen.types :as t]
            [thrift-clj.thrift.services :as s]
            [thrift-clj.utils.symbols :as u]
            [thrift-clj.utils.namespaces :as nsp]))

;; ## Iface
;;
;; The Service Interface is located at `<Service>$Iface`. It can be
;; accessed by creating a namespace that contains a single function
;; for each interface method, transparently converting between 
;; Clojure and Thrift representations.

(defn generate-thrift-iface-import
  [service-class iface-alias]
  (let [cls (u/full-class-symbol service-class)
        iface-cls (u/inner cls "Iface")
        mth (s/thrift-service-methods service-class)
        param-syms (repeatedly gensym)]
    `(do 
       (nsp/internal-ns 
         ~iface-cls
         ~@(for [{:keys[name params]} mth]
             (let [params (take (count params) param-syms)]
               `(defn ~(symbol name)
                  [client# ~@params]
                  (t/->clj
                    (. client# ~(symbol name) 
                       ~@(map #(list `t/->thrift %) params)))))))
       (nsp/internal-ns-require '~iface-cls '~iface-alias))))
