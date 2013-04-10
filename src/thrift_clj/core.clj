(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [load])
  (:use [potemkin :only [import-macro import-fn]]
        clojure.tools.logging)
  (:require [thrift-clj.core.thrift-types :as t]
            [thrift-clj.core.thrift-services :as s]
            [thrift-clj.core.thrift-clients :as c]
            [thrift-clj.server :as srv]
            [thrift-clj.reflect :as reflect]))

;; ## Imported

(import-macro s/defservice)
(import-macro c/create-client)
(import-macro c/with-client)
(import-fn c/start-client!)
(import-fn c/stop-client!)
(import-fn t/clj->thrift)
(import-fn t/thrift->clj)
(import-fn srv/start-server!)
(import-fn srv/stop-server!)

;; ## Load Certain Resources

(defn- load-class
  "Load Class with Error Logging."
  [n]
  (try
    (Class/forName n)
    (catch Exception ex
      (error ex "Could not load Class:" n)
      (throw ex))))

(defmacro import-types
  "Import the given Thrift Types making them accessible as Clojure Types.
   There are three types of specification formats:
   - `package.Class`: load exactly this type, using the name `Class`
   - `[package Class1 Class2 ...]`: load the given types from the package, using class names
     as type names
   - `[package.Class :as N]`: load the given type, using the name `N`
  "
  [& types]
  (let [type-map (reduce 
                   (fn [m t]
                     (cond (symbol? t) 
                             (assoc m (load-class (name t)) nil)
                           (and (vector? t) (= (second t) :as))
                             (assoc m (load-class (name (first t))) (nth t 2))
                           (vector? t) 
                             (reduce
                               (fn [m c]
                                 (assoc m (load-class (str (name (first t)) "." (name c))) nil))
                               m (rest t))
                           :else m))
                   {} types)]
    `(do
       ~@(t/import-thrift-types type-map)
       true)))

(defmacro import-all-types
  "Import all types that reside in a package with one of the given prefixes."
  [& packages]
  (let [types (reflect/thrift-types (map str packages))]
    `(do
       ~@(t/import-thrift-types types)
       true)))

;; ## Main Macros

(defmacro load-services
  "Load Thrift Services from the given packages."
  [& packages]
  (let [packages (map str packages)]
    `(do ~@(s/generate-thrift-services packages) true)))

(defmacro load
  "Load all Thrift Entities in the given packages."
  [& packages]
  `(do
     (load-types ~@packages)
     (load-services ~@packages)))
