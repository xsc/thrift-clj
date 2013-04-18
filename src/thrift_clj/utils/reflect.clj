(ns ^{ :doc "Using Reflection to get Thrift Entities."
       :author "Yannick Scherer" }
  thrift-clj.utils.reflect
  (:import (org.reflections Reflections ReflectionUtils)
           (org.reflections.scanners Scanner SubTypesScanner TypeElementsScanner)
           (org.reflections.util ClasspathHelper ConfigurationBuilder FilterBuilder)))

;; ## Google Reflection Helpers

(defn- ^FilterBuilder create-prefix-filter
  "Create FilterBuilder that filters by Prefix."
  [prefixes]
  (let [builder (FilterBuilder.)]
    (doseq [prefix prefixes]
      (.include builder (FilterBuilder/prefix prefix)))
    builder))

(defn- create-classpath-urls
  "Create set of URLs matching the given package."
  [packages]
  (mapcat
    #(ClasspathHelper/forPackage % nil)
    packages))

(defn- ^"[Lorg.reflections.scanners.Scanner;" create-scanners
  "Create Scanners needed for usage of `Reflections.getSubTypesOf`."
  [scanners]
  (->>
    (concat
      [(SubTypesScanner.)]
      scanners)
    (into-array Scanner)))

(defn- ^ConfigurationBuilder create-configuration
  "Create Configuration needed for analysis of the given packages."
  [packages scanners]
  (let [^FilterBuilder f (create-prefix-filter packages)
        ^java.util.List urls (create-classpath-urls packages)
        ^"[Lorg.reflections.scanners.Scanner;" s (create-scanners scanners)]
    (doto (ConfigurationBuilder.)
      (.filterInputsBy f)
      (.setUrls urls)
      (.setScanners s))))

(defn- ^Reflections create-reflection*
  "Create `Reflections` object capable of examining the given packages."
  [packages & scanners]
  (let [config (create-configuration packages scanners)]
    (Reflections. config)))

(def create-reflection (memoize create-reflection*))

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
  [^Class class ^String name]
  (first (filter #(= (.getSimpleName ^Class %) name) (.getDeclaredClasses class))))

;; ## Scanning Packages

(defn find-subtypes
  "Find types that implement or extend the given base type."
  [base-type packages]
  (let [^Reflections reflect (create-reflection packages)]
    (.getSubTypesOf reflect base-type)))
