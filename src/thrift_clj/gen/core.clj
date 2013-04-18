(ns ^{ :doc "Core Definitions for Import Generation"
       :author "Yannick Scherer" }
  thrift-clj.gen.core)

;; ## Types

(defprotocol Value
  "Protocol for Values."
  (to-thrift* [this]
    "Convert Value to Thrift Representation if possible.")
  (to-thrift-unchecked* [this]
    "Convert Value to Thrift Representation without checking optionality & co.")
  (to-clj* [this]
    "Convert Value to Clojure Representation if possible."))

(defn ->thrift
  [v]
  (if (satisfies? Value v)
    (to-thrift* v)
    v))

(defn ->thrift-unchecked
  [v]
  (if (satisfies? Value v)
    (to-thrift-unchecked* v)
    v))

(defn ->clj
  [v]
  (if (satisfies? Value v)
    (to-clj* v)
    v))
