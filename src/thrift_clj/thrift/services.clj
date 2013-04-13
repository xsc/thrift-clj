(ns ^{ :doc "Thrift Service Analysis."
       :author "Yannick Scherer" }
  thrift-clj.thrift.services
  (:require [thrift-clj.utils.reflect :as reflect :only [inner-class]]))

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
