(defproject thrift-clj "0.1.2"
  :description "Clojure and Thrift working hand in hand."
  :url "https://github.com/xsc/thrift-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.reflections/reflections "0.9.9-RC1"]
                 [org.apache.thrift/libthrift "0.9.0" :exclusions [org.slf4j/slf4j-api]]
                 [javax.servlet/servlet-api "2.5"]
                 [potemkin "0.3.1"]]
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :exclusions [org.clojure/clojure]

  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.5"]]}
             :test {:dependencies [[midje "1.5.1"]]
                    :plugins [[lein-midje "3.0.1"]
                              [lein-thriftc "0.1.0"]]
                    :prep-tasks ["thriftc"]
                    :test-paths ["test-thrift/clj"]
                    :thriftc {:source-paths ["test-thrift/thrift"]}}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}
             :doc {:plugins [[codox "0.6.4"]
                             [lein-marginalia "0.7.1"]]
                   :codox {:sources ["src/thrift_clj"]
                           :output-dir "doc/autodoc"}}
             :reflection {:warn-on-reflection true}}

  :aliases {"midje-all" ["with-profile" "dev,test,1.4:dev,test,1.5:dev,test,1.6" "midje"]
            "doc-marginalia" ["with-profile" "doc" "marg" "-d" "doc/autodoc" "-f" "marginalia.html"]
            "doc-codox" ["with-profile" "doc" "doc"]})
