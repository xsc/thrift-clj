(ns ^{ :doc "Namespace Building Utilities"
       :author "Yannick Scherer" }
  thrift-clj.utils.namespace)

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
    (remove-ns (:ns-name ns-data))))

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
  (when-not (@internal-namespaces ns-key)
    (let [current-ns (ns-name *ns*)
          unique-name (gensym "ns")]
      `(do
         (ns ~unique-name)
         ~@body
         (in-ns '~current-ns)
         (internal-ns-add '~ns-key '~unique-name)
         nil))))

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
    (require [(:ns-name ns-data) :as ~ns-alias])
    (throw (Exception. (str "No such internal Namespace: " ns-key)))))

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
                     (let [cn (symbol (.getName r))]
                       `(import '~cn))))
                 (throw (Exception. (str "No such Type in internal Namespace <" n ">: " t)))))))
      (throw (Exception. (str "No such internal Namespace: " ns-key))))))




