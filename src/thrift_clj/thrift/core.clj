(ns ^{ :doc "Wrapping Thrift Data Structures"
       :author "Yannick Scherer" }
  thrift-clj.thrift.core
  (:require [thrift-clj.utils.reflect :as reflect])
  (:import (org.apache.thrift TBase TProcessor)))

;; ## Inspect Namespaces

(defn thrift-types
  "Get set of Classes implementing `org.apache.thrift.TBase`, i.e. Thrift-
   generated Types."
  [packages]
  (->> packages
    (reflect/find-subtypes TBase)
    (filter #(nil? (.getDeclaringClass %)))))

(defn thrift-processors
  "Get set of Classes implementing `org.apache.thrift.TProcessor`, i.e.
   Thrift-generated Processors."
  [packages]
  (reflect/find-subtypes TProcessor))

(defn thrift-service-methods
  "Get seq of methods defined in a service."
  [service]
  (when-let [iface (reflect/inner-class service "Iface")]
    (map 
      (fn [m]
        (-> {}
          (assoc :name (.getName m))
          (assoc :params (into [] (.getParameterTypes m)))
          (assoc :returns (.getReturnType m))))
      (.getMethods iface))))
