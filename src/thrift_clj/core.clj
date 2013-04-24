(ns ^{ :doc "Thrift/Clojure Integration"
       :author "Yannick Scherer" }
  thrift-clj.core
  (:refer-clojure :exclude [import])
  (:use [potemkin :only [import-macro import-fn]]
        clojure.tools.logging)
  (:require [thrift-clj.gen.core :as gen]
            [thrift-clj.gen.types :as t]
            [thrift-clj.gen.services :as s]
            [thrift-clj.gen.clients :as c]
            [thrift-clj.gen.iface :as i]

            [thrift-clj.client :as client]
            [thrift-clj.server :as server]
            [thrift-clj.transports :as transport]

            [thrift-clj.thrift.core :as thr]))

;; ## Concept
;;
;; __thrift-clj__ uses Clojure's Java interoperability to access Classes generated
;; by Thrift. It will wrap each class (when imported using thrift-clj's macros) in
;; a separate namespace, aliasing it as desired, importing types, creating wrapper
;; functions, etc... 

;; ## Imported

(import-macro s/service)
(import-macro s/defservice)

(import-fn client/connect!)
(import-fn client/disconnect!)

(import-fn transport/framed)
(import-fn transport/fast-framed)
(import-fn transport/tcp)
(import-fn transport/tcp-async)
(import-fn transport/http)
(import-fn transport/streams)
(import-fn transport/blocking-server-transport)
(import-fn transport/nonblocking-server-transport)

(import-fn gen/->thrift)
(import-fn gen/->clj)

(import-fn server/single-threaded-server)
(import-fn server/multi-threaded-server)
(import-fn server/nonblocking-server)
(import-fn server/serve-and-block!)
(import-fn server/serve!)
(import-fn server/stop!)

;; ## Exceptions

(defmacro throw
  "Throw Exception. May take the Clojure representation of a Thrift Exception."
  [ex & args]
  `(throw (->thrift ~ex) ~@args))

(defmacro try
  "Try executing the given Forms catching Exceptions that can be identified by their
   Clojure equivalent."
  [& forms]
  (let [[forms handlers] (split-with 
                           (fn [f]
                             (not
                               (and (seq? f)
                                    (symbol? (first f))
                                    (contains? #{"catch" "finally"} (name (first f))))))
                           forms)
        handlers (group-by (comp keyword name first) handlers)
        e (gensym "e-")
        ex (gensym "ex-")]
    `(try 
       (do ~@forms)
       (catch Exception ~e
         (let [~ex (->clj ~e)]
           (cond ~@(mapcat
                     (fn [[_ cls sym & body]]
                       (vector
                         `(instance? ~cls ~ex) `(let [~sym ~ex]
                                                  (do ~@body))
                         `(instance? ~cls ~e) `(let [~sym ~e]
                                                 (do ~@body))))
                     (:catch handlers))
                 :else (throw ~e))))
       ~@(seq (:finally handlers)))))

;; ## Load Certain Resources

;; ### Helpers

(defn- load-class
  "Load Class with Error Logging."
  [n]
  (try
    (Class/forName n)
    (catch Exception ex
      (error ex "Could not load Class:" n)
      (throw ex))))

(defn- generate-class-map
  [specs]
  (reduce 
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
    {} specs))

;; ### Types

(defmacro import-types
  "Import the given Thrift Types making them accessible as Clojure Types of the same name."
  [& types]
  (let [type-map (generate-class-map types)
        types (keys type-map)] ;; no aliases allowed
    `(do
       ~@(t/generate-thrift-type-imports types)
       true)))

(defmacro import-all-types
  "Import all types that reside in a package with one of the given prefixes."
  [& packages]
  (let [types (thr/thrift-types (map name packages))]
    `(do
       ~@(t/generate-thrift-type-imports types)
       true)))

;; ### Services

(defmacro import-services
  "Import the given Thrift Services making them accessible via `defservice`.
   There are three types of specification formats:

   - `package.Class`: load exactly this service, using the name `Class`
   - `[package Class1 Class2 ...]`: load the given services from the package, using class names
     as type names
   - `[package.Class :as N]`: load the given service, using the name `N`
  "
  [& services]
  (let [service-map (generate-class-map services)]
    `(do
       ~@(s/generate-thrift-service-imports service-map)
       true)))

(defmacro import-all-services
  "Import all services that reside in a package with one of the given prefixes."
  [& packages]
  (let [services (reduce
                   #(assoc %1 %2 nil)
                   {}
                   (thr/thrift-services (map name packages)))]
    `(do
       ~@(s/generate-thrift-service-imports services)
       true)))

;; ### Clients

(defmacro import-clients
  "Import the given Thrift Clients making them accessible via `defservice`.
   There are three types of specification formats:

   - `package.Class`: load exactly this client, using the name `Class`
   - `[package Class1 Class2 ...]`: load the given client from the package, using class names
     as type names
   - `[package.Class :as N]`: load the given client, using the name `N`
  "
  [& services]
  (let [service-map (generate-class-map services)]
    `(do
       ~@(c/generate-thrift-client-imports service-map)
       true)))

(defmacro import-all-clients
  "Import all services that reside in a package with one of the given prefixes."
  [& packages]
  (let [services (reduce
                   #(assoc %1 %2 nil)
                   {}
                   (thr/thrift-services (map name packages)))]
    `(do
       ~@(c/generate-thrift-client-imports services)
       true)))

;; ### Iface

(defmacro import-iface
  "Import the given Thrift Iface without creating Services/Clients to handle it."
  [& services]
  (let [service-map (generate-class-map services)]
    `(do
       ~@(i/generate-thrift-iface-imports service-map)
       true)))

;; ### Everyhting

(defmacro import-all
  "Load all Thrift Entities in the given packages."
  [& packages]
  `(do
     (import-all-types ~@packages)
     (import-all-services ~@packages)
     (import-all-clients ~@packages)))

;; ### Main Macro

(defmacro import
  [& specs]
  `(do
     ~@(for [[k & rst] specs]
         (case k
           :services `(import-services ~@rst)
           :types `(import-types ~@rst)
           :clients `(import-clients ~@rst)
           :packages `(import-all ~@rst)
           (throw (Exception. (str "Invalid Key in `import': " k)))))))
