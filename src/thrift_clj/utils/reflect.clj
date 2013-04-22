(ns ^{ :doc "Using Reflection to get Thrift Entities."
       :author "Yannick Scherer" }
  thrift-clj.utils.reflect
  (:import (org.reflections Reflections ReflectionUtils)
           (org.reflections.scanners Scanner SubTypesScanner TypeElementsScanner)
           (org.reflections.util ClasspathHelper ConfigurationBuilder FilterBuilder)))

;; ## Google Reflection Helpers

(defn- create-prefix-filter
  "Create FilterBuilder that filters by Prefix."
  ^FilterBuilder 
  [prefixes]
  (let [builder (FilterBuilder.)]
    (doseq [prefix prefixes]
      (.include builder (FilterBuilder/prefix prefix)))
    builder))

(defn- create-classpath-urls
  "Create set of URLs matching the given package."
  ^java.util.List
  [packages]
  (mapcat
    #(ClasspathHelper/forPackage % nil)
    packages))

(defn- create-scanners
  "Create Scanners needed for usage of `Reflections.getSubTypesOf`."
  ^"[Lorg.reflections.scanners.Scanner;" 
  [scanners]
  (->>
    (concat
      [(SubTypesScanner.)]
      scanners)
    (into-array Scanner)))

(defn- create-configuration
  "Create Configuration needed for analysis of the given packages."
  ^ConfigurationBuilder 
  [packages scanners]
  (let [f (create-prefix-filter packages)
        urls (create-classpath-urls packages)
        s (create-scanners scanners)]
    (doto (ConfigurationBuilder.)
      (.filterInputsBy f)
      (.setUrls urls)
      (.setScanners s))))

(defn create-reflection
  "Create `Reflections` object capable of examining the given packages."
  ^Reflections
  [packages & scanners]
  (let [config (create-configuration packages scanners)]
    (Reflections. config)))

;; ## General Reflection Helpers

(defn class-constructors
  "Returns a seq of vectors describing the types of constructor
   parameters needed for this Class."
  [^Class class]
  (let [constructors (.getConstructors class)]
    (map
      (fn [constr]
        (into [] (.getParameterTypes ^java.lang.reflect.Constructor constr)))
      constructors)))

(defn inner-class
  ^Class
  [^Class class ^String name]
  (first (filter #(= (.getSimpleName ^Class %) name) (.getDeclaredClasses class))))

;; ## Scanning Packages

(defn find-subtypes
  "Find types that implement or extend the given base type."
  [base-type packages]
  (let [reflect (create-reflection packages)]
    (.getSubTypesOf reflect base-type)))
