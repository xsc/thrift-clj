(defproject thrift-clj "0.3.1"
  :description "Clojure and Thrift working hand in hand."
  :url "https://github.com/xsc/thrift-clj"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.reflections/reflections "0.9.10"]
                 [org.apache.thrift/libthrift "0.9.3" :exclusions [org.slf4j/slf4j-api]]
                 [javax.servlet/servlet-api "2.5"]
                 [potemkin "0.4.1"]]
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :exclusions [org.clojure/clojure]

  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.1.3"]
                                  [midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-thriftc "0.2.3"]]
                   :hooks [leiningen.thriftc]
                   :test-paths ["test-thrift/clj"]
                   :javac-opts ["-source" "1.6" "-target" "1.6"]
                   :thriftc {:source-paths ["test-thrift/thrift"]}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :doc {:plugins [[codox "0.8.13"]]
                   :codox {:sources ["src/thrift_clj"]
                           :include [thrift-clj.core]}}
             :reflection {:warn-on-reflection true}}

  :aliases {"all" ["with-profile" "+1.5:+1.6:+1.7"]
            "test" "midje"
            "doc" ["with-profile" "doc" "doc"]})
