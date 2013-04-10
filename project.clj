(defproject thrift-clj "0.1.0-alpha1"
  :description "Clojure and Thrift working hand in hand."
  :url "https://github.com/xsc/thrift-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.reflections/reflections "0.9.9-RC1"]
                 [org.apache.thrift/libthrift "0.9.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [potemkin "0.2.1"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.5.2"]]}
             :example {:thrift-java-path "example/java"
                       :thrift-source-path "example/thrift"
                       :java-source-paths ["example/java"]
                       :plugins [[lein-thrift "0.1.0"]]}}
  :aliases {"example-repl" ["with-profile" "dev,example" "do" "thrift" "," "repl"]})
