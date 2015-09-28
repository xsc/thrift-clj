(ns ^{ :doc "Namespace Building Utilities"
       :author "Yannick Scherer" }
  thrift-clj.utils.namespaces)

;; ## Reload Detection

(defonce ^:private indicator-once (gensym))

(defmacro def-reload-indicator
  "Creates a function that takes an ID (e.g. symbol, keyword) as parameter and decides
   whether the ID has to be reloaded by observing whether the ID has been loaded since
   the namesapce the indicator lies in has been reloaded."
  [id]
  `(let [indicator# (gensym)]
     (defonce ~(vary-meta indicator-once assoc :private true) (atom {}))
     (defn ~(vary-meta id assoc :private true)
       [v#]
       (if-let [i# (get @~indicator-once v#)]
         (when-not (= i# indicator#)
           (swap! ~indicator-once assoc v# indicator#)
           true)
         (do
           (swap! ~indicator-once assoc v# indicator#)
           false)))))

;; ## Namespace Container

(defonce
  ^{:private true
    :doc "Namespaces that were already created and are available for `require` and `use`."}
  internal-namespaces
  (atom {}))

(defn internal-ns-remove
  "Remove internal Namespace."
  [ns-key]
  (when-let [ns-data (@internal-namespaces ns-key)]
    (swap! internal-namespaces dissoc ns-key)
    #_(remove-ns (:ns-name ns-data))))

(defn internal-ns-add
  "Add internal Namespace."
  [ns-key ns-id]
  (let [ns-data (-> {}
                  (assoc :ns-name ns-id))]
    (swap! internal-namespaces
           assoc ns-key ns-data)))

(defmacro internal-ns
  "Create new internal Namespace."
  [ns-key & body]
  (let [current-ns (ns-name *ns*)
        unique-name (symbol (str "ns" (.hashCode (str ns-key))))]
    `(do
       (ns ~unique-name
         (:refer-clojure :only ~'[fn find-ns]))
       ~@body
       (in-ns '~current-ns)
       (internal-ns-add '~ns-key '~unique-name)
       nil)))

(defn internal-namespace-exists?
  "Does a given internal Namespace exist?"
  [ns-key]
  (boolean (@internal-namespaces ns-key)))

(defn internal-ns-require
  "Require internal Namespace identified by the given key using a mandatory alias."
  [ns-key ns-alias]
  (when-not ns-alias
    (throw (Exception. "`internal-ns-require` needs an alias!")))
  (if-let [ns-data (@internal-namespaces ns-key)]
    (let [ns-id (:ns-name ns-data)]
      (try
        (refer ns-id)
        (alias ns-alias ns-id)
        (catch Exception ex
          (throw (Exception. (str "Failed to require internal Namespace for: " ns-key " (" ns-id ")\n"
                                  (.getMessage ex)))))))
    (throw (Exception. (str "No internal Namespace for: " ns-key)))))

(defn internal-ns-refer
  "Refers to the named vars of the given internal Namespace."
  [ns-key & symbols]
  (if-let [ns-data (@internal-namespaces ns-key)]
    (let [ns-id (:ns-name ns-data)]
      (try
        (refer ns-id :only symbols)
        (catch Exception ex
          (throw (Exception. (str "Failed to require internal Namespace for: " ns-key " (" ns-id ")\n"
                                  (.getMessage ex)))))))
    (throw (Exception. (str "No internal Namespace for: " ns-key)))))

(defmacro internal-ns-import
  "Import Classes from the given internal Namespace."
  [ns-key & types]
  (when (seq types)
    (if-let [ns-data (@internal-namespaces ns-key)]
      (let [n (:ns-name ns-data)]
        `(do
           ~@(for [t types]
               (or
                 (when-let [r (ns-resolve n t)]
                   (when (class? r)
                     (let [cn (symbol (.getName ^Class r))]
                       `(import '~cn))))
                 (throw (Exception. (str "No such Type in internal Namespace <" n ">: " t)))))))
      (throw (Exception. (str "No such internal Namespace: " ns-key))))))
