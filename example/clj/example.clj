(ns example
  (:require [thrift-clj.core :as thrift]))

;; Import Thrift Classes
(thrift/import
  (:types [org.example Person Name])
  (:services [org.example.PersonIndex :as PS])
  (:clients [org.example.PersonIndex :as PI]))

;; Implement the Service
(thrift/defservice person-index-service PS
  :let [person-db (atom {})]
  (store [{:keys[name age id] :as p}]
    (println "Storing Person:")
    (println "  First Name:" (:firstName name))
    (println "  Last Name:" (:lastName name))
    (println "  Age:" age)
    (swap! person-db assoc id p) 
    true)
  (getPerson [id]
    (get @person-db id)))

;; Start a Server

;; Prepare Client and Data
(def client (thrift/create-client PI :socket "localhost" 7007))
(def p (Person. 0 (Name. "Some" "One") 99))

;; Go!
(defn -run
  []
  (when-let [server (thrift/multi-threaded-server person-index-service :socket 7007)]
    (try
      (future (thrift/start-server! server))
      (with-open [c (thrift/connect! client)]
        (PI/store c p)
        (println "Trying to retrieve Person ...")
        (let [p' (PI/getPerson c (int 0))]
          (println p' (if (= p p') "[matches sent Person]" "")))
        (println "Done."))
      (finally
        (thrift/stop-server! server)))))
