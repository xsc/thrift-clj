(ns example
  (:require [thrift-clj.core :as thrift]))

;; Import Thrift Classes
(thrift/import
  (:types org.example.Person)
  (:services [org.example.PersonIndex :as PS])
  (:clients [org.example.PersonIndex :as PI]))

;; Implement the Service
(thrift/defservice person-index-service PS
  :let [person-db (atom {})]
  (store [{:keys[firstName lastName age] :as p}]
    (println "Storing Person:")
    (println "  First Name:" firstName)
    (println "  Last Name:" lastName)
    (println "  Age:" age)
    (swap! person-db update-in [lastName] conj p) 
    true)
  (getPerson [lastName]
    (first (@person-db lastName))))

;; Start a Server
(def server (thrift/single-threaded-server person-index-service :socket 7007))
(future (thrift/start-server! server))

;; Prepare Client and Data
(def client (thrift/create-client PI :socket "localhost" 7007))
(def p (Person. "Some" "One" 99))

;; Go!
(with-open [c (thrift/connect! client)]
  (PI/store c p)
  (println "Trying to retrieve Person ...")
  (let [p' (PI/getPerson c "One")]
    (println p' (if (= p p') "[matches sent Person]" "")))
  (println "Done."))

;; Cleanup
(thrift/stop-server! server)
