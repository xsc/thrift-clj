(defproject thrift-clj "0.2.2-SNAPSHOT"
  :description "Clojure and Thrift working hand in hand."
  :url "https://github.com/xsc/thrift-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.reflections/reflections "0.9.9"]
                 [org.apache.thrift/libthrift "0.9.1" :exclusions [org.slf4j/slf4j-api]]
                 [javax.servlet/servlet-api "2.5"]
                 [potemkin "0.3.11"]]
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :exclusions [org.clojure/clojure]

  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.1.2"]
                                  [midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-thriftc "0.2.1"]]
                   :hooks [leiningen.thriftc]
                   :test-paths ["test-thrift/clj"]
                   :javac-opts ["-source" "1.6" "-target" "1.6"]
                   :thriftc {:source-paths ["test-thrift/thrift"]}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}
             :doc {:plugins [[codox "0.6.4"]
                             [lein-marginalia "0.7.1"]]
                   :codox {:sources ["src/thrift_clj"]
                           :output-dir "doc/autodoc"}}
             :reflection {:warn-on-reflection true}}

  :aliases {"all" ["with-profile" "+1.5:+1.6:+1.7"]
            "test" "midje"
            "doc-marginalia" ["with-profile" "doc" "marg" "-d" "doc/autodoc" "-f" "marginalia.html"]
            "doc-codox" ["with-profile" "doc" "doc"]})
