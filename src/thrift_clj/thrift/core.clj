(ns ^{ :doc "Wrapping Thrift Data Structures"
       :author "Yannick Scherer" }
  thrift-clj.thrift.core
  (:require [thrift-clj.utils.reflect :as reflect])
  (:import (org.apache.thrift TBase TProcessor TEnum)))

;; ## Inspect Namespaces

(defn thrift-types
  "Get set of Classes implementing `org.apache.thrift.TBase`, i.e. Thrift-
   generated Types."
  [packages]
  (->> packages
    (reflect/find-subtypes TBase)
    (filter #(nil? (.getDeclaringClass %)))))

(defn thrift-enums
  "Get set of Classes implementing `org.apache.thrift.TEnum`, i.e. Thrift-
   generated Enums."
  [packages]
  (reflect/find-subtypes packages TEnum))

(defn thrift-processors
  "Get set of Classes implementing `org.apache.thrift.TProcessor`, i.e.
   Thrift-generated Processors."
  [packages]
  (reflect/find-subtypes packages TProcessor))

(defn thrift-services
  "Get set of classes containing an `org.apache.thrift.TProcessor`, i.e.
   Thrift-generated Services."
  [packages]
  (let [processors (thrift-processors packages)
        services (map #(.getDeclaringClass %) processors)]
    (set (filter (complement nil?) services))))
