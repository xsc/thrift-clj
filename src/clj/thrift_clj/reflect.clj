(ns ^{ :doc "Using Reflection to get Thrift Entities."
       :author "Yannick Scherer" }
  thrift-clj.reflect
  (:import (org.reflections Reflections ReflectionUtils)
           (org.reflections.scanners Scanner SubTypesScanner TypeElementsScanner)
           (org.reflections.util ClasspathHelper ConfigurationBuilder FilterBuilder)
           (org.apache.thrift TBase TProcessor)))

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
  (doto (ConfigurationBuilder.)
    (.filterInputsBy (create-prefix-filter packages))
    (.setUrls (create-classpath-urls packages))
    (.setScanners (create-scanners scanners))))

(defn- ^Reflections create-reflection
  "Create `Reflections` object capable of examining the given packages."
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
        (into [] (.getParameterTypes constr)))
      constructors)))

(defn full-class-symbol
  "Get Symbol representing Class (including Package)."
  [^Class class]
  (symbol (.getName class)))

(defn class-symbol
  "Get Symbol representing Class (without Package)."
  [^Class class]
  (symbol (.getSimpleName class)))

;; ## Thrift-specific Functions

(defn thrift-types
  "Get set of Classes implementing `org.apache.thrift.TBase`, i.e. Thrift-
   generated Types."
  [packages]
  (let [reflect (create-reflection packages)]
    (.getSubTypesOf reflect TBase)))

(defn thrift-processors
  "Get set of Classes implementing `org.apache.thrift.TProcessor`, i.e.
   Thrift-generated Processors."
  [packages]
  (let [reflect (create-reflection packages)]
    (.getSubTypesOf reflect TProcessor)))

(defn thrift-services
  "Get set of classes containing an `org.apache.thrift.TProcessor`, i.e.
   Thrift-generated Services."
  [packages]
  (let [processors (thrift-processors packages)]
    (set (map #(.getDeclaringClass %) processors))))
