(defproject thrift-clj "0.1.0-SNAPSHOT"
  :description "Clojure and Thrift working hand in hand."
  :url "https://github.com/xsc/thrift-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.reflections/reflections "0.9.9-RC1"]
                 [org.apache.thrift/libthrift "0.9.0"]
                 [javax.servlet/servlet-api "2.5"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"])
