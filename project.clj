(defproject thrift-clj "0.1.1-SNAPSHOT"
  :description "Clojure and Thrift working hand in hand."
  :url "https://github.com/xsc/thrift-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.reflections/reflections "0.9.9-RC1"]
                 [org.apache.thrift/libthrift "0.9.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.slf4j/slf4j-log4j12 "1.5.2"]
                 [potemkin "0.2.2"]]

  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.0.1"]]}
             :doc {:plugins [[codox "0.6.4"]
                             [lein-marginalia "0.7.1"]]
                   :codox {:sources ["src/thrift_clj"]
                           :output-dir "doc/autodoc"}}
             :test-reflection { :warn-on-reflection true }
             :test-all {:plugins [[lein-thriftc "0.1.0"]]
                        :prep-tasks ["thriftc"]
                        :test-paths ["test-thrift/clj"]
                        :thriftc {:source-paths ["test-thrift/thrift"]}}}

  :aliases {"midje-all" ["with-profile" "dev,test-all" "midje"]
            "midje-all+reflection" ["with-profile" "dev,test-all,test-reflection" "midje"]
            "doc-marginalia" ["with-profile" "doc" "marg" "-d" "doc/autodoc" "-f" "marginalia.html"]
            "doc-codox" ["with-profile" "doc" "doc"]})
