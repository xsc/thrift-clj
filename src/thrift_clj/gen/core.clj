(ns ^{ :doc "Core Definitions for Import Generation"
       :author "Yannick Scherer" }
  thrift-clj.gen.core)

;; ## Types

(defprotocol Value
  "Protocol for Values."
  (->thrift* [this]
    "Convert Value to Thrift Representation if possible.")
  (->thrift-unchecked* [this]
    "Convert Value to Thrift Representation without checking optionality & co.")
  (->clj* [this]
    "Convert Value to Clojure Representation if possible."))

(defn ->thrift
  [v]
  (if (satisfies? Value v)
    (->thrift* v)
    v))

(defn ->thrift-unchecked
  [v]
  (if (satisfies? Value v)
    (->thrift-unchecked* v)
    v))

(defn ->clj
  [v]
  (if (satisfies? Value v)
    (->clj* v)
    v))
