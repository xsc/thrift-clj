(ns ^{ :doc "Symbol Utility Functions"
       :author "Yannick Scherer" }
  thrift-clj.utils.symbols)

(defn inner
  "Create symbol representing an inner class."
  [outer-class inner-class]
  (symbol (str outer-class "$" inner-class)))

(defn static
  "Create symbol representing a static method."
  [class method]
  (symbol (str class "/" method)))

(defn full-class-symbol
  "Get Symbol representing Class (including Package)."
  [^Class class]
  (symbol (.getName class)))

(defn class-symbol
  "Get Symbol representing Class (without Package)."
  [^Class class]
  (symbol (.getSimpleName class)))
