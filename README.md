# thrift-clj

Using Thrift from Clojure as if it was Clojure.

__Note__: This is an alpha release. The API might change at any point (but, at least for the most prominent parts, 
probably won't).

## Usage

thrift-clj is available via [Clojars](http://clojars.org/thrift-clj).

__Leiningen__

```clojure
[thrift-clj "0.1.0-alpha1"]
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
    bool store(1:Person p)
}
```

Compile the Thrift file (`thrift --gen java -out <Path> test.thrift`) and add `<Path>` to 
Leiningens classpath:

```clojure
...
  :java-source-paths ["<Path>" ...]
...
```

__REPL__

```clojure
(require '[thrift-clj.core :as thrift])
(thrift/import
  (:types org.example.Person)
  (:services org.example.PersonIndex)
  (:clients [org.example.PersonIndex :as PIClient]))
 
(thrift/defservice person-index-service
  PersonIndex
  (store [{:keys[firstName lastName age]}]
    (println "Storing Person:")
    (println "  First Name:" firstName)
    (println "  Last Name:" lastName)
    (println "  Age:" age)
    true))
 
(def server (thrift/single-threaded-server person-index-service :socket 7007))
(def client (thrift/create-client PIClient :socket "localhost" 7007))
(future (thrift/start-server! server))
(thrift/start-client! client)
 
(def p (Person. "Yannick" "Scherer" 24))
(thrift/->thrift p) ;; => org.example.Person<...>
(PIClient/store client p)
;; Storing Person:
;;   First Name: Yannick
;;   Last Name: Scherer
;;   Age: 24
;; => true
 
(thrift/stop-client! client)
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
