# thrift-clj

Using Thrift from Clojure as if it was Clojure.

__Note__: This is an alpha release. The API might change at any point (but, at least for the most prominent parts, 
probably won't).

## Usage

thrift-clj is available via [Clojars](http://clojars.org/thrift-clj).

__Leiningen__

```clojure
[thrift-clj "0.1.0-alpha2"]
```

__Note__: Tested with the Thrift 0.9.0 compiler. Since this depends massively on the generated code, make sure to use
that version (or any other one that was tested with this library).

## Example

__test.thrift__

```thrift
namespace java org.example

struct Person {
  1: optional string firstName,
  2: string lastName,
  3: byte age
}

service PersonIndex {
    bool store(1:Person p),
    Person getPerson(1:string lastName)
}
```

Compile the Thrift file (`thrift --gen java -out <Path> test.thrift`) and add `<Path>` to 
Leiningens classpath:

```clojure
...
  :java-source-paths ["<Path>" ...]
...
```

__example.clj__

```clojure
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
```

## Roadmap

- Have a look at Lists/Sets/Enums/...
- wrappers around Protocols (to encode/decode values directly to/from byte arrays)
- more protocols/servers/clients
- a Leiningen Plugin that operates with thrift-clj in mind
- tests & documentationn
- ...

## Related Work/Inspiration

- [Apache Thrift](https://github.com/apache/thrift)
- [Plaid Penguin](https://github.com/ithayer/plaid-penguin)
- [lein-thrift](https://github.com/kurtharriger/lein-thrift)

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
