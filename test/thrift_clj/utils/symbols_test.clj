(ns ^{ :doc "Tests for Symbol Utilities"
       :author "Yannick Scherer" }
  thrift-clj.utils.symbols-test
  (:use midje.sweet
        thrift-clj.utils.symbols)
  (:import java.util.HashMap))

(fact "about symbol utilities"
  (let [base 'pckg.Object]
    (inner base "Inner") => 'pckg.Object$Inner
    (static base "Run") => 'pckg.Object/Run
    (full-class-symbol HashMap) => 'java.util.HashMap
    (class-symbol HashMap) => 'HashMap))
