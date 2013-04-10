# thrift-clj

Using Thrift from Clojure as if it was Clojure.

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
(thrift/import-types org.example.Person)
```

This will make the type `Person` directly accessible in Clojure.

```clojure
(def some-person (Person. "Some" "One" 24))
;; => #user.Person{:firstName Some, :lastName One, :age 24}

(def some-thrift-person (thrift/->thrift some-person))
;; => #<Person Person(firstName:Some, lastName:One, age:24)>

(class some-thrift-person)
;; => org.example.Person

(thrift/->clj some-thrift-person)
;; => #user.Person{:firstName Some, :lastName One, :age 24}
``` 

Now, let's tackle the service `PersonIndex`. We can implement it using only Clojure:

```clojure
(thrift/import-services [org.example.PersonIndex :as PersonIndex])
(thrift/defservice person-index
  PersonIndex
  (store [p]
    (println "Storing Person:" (:firstName p) (:lastName p))
    true))
```

Get it up and running with:

```clojure
(def server (thrift/single-threaded-server person-index :socket 7007))
(future (thrift/start-server! server))
```

Clients can be created similarly:

```clojure
(thrift/import-clients [org.example.PersonIndex :as PersonIndexClient])
(def client (thrift/create-client PersonIndexClient :socket "localhost" 7007))
(thrift/start-client! client)
```

There are two ways to call a service method: either directly using the Java methods 
(which requires all parameters to have the right type), or using the functions generated
in a namespace aliased with whatever was given in the `:as` part of `import-clients`:

```clojure
(.store client some-person)             ;; => Exception!!
(.store client some-thrift-person)      ;; => true
(PersonIndexClient/store client some-person) ;; => true
```

Cleanup

```clojure
(thrift/stop-client! client)
(thrift/stop-server! server)
```

## Roadmap

- Have a look at Lists/Sets/...
- wrappers around Protocols (to encode/decode values directly to/from byte arrays)
- ...

## Related Work/Inspiration

- [Apache Thrift](https://github.com/apache/thrift)
- [Plaid Penguin](https://github.com/ithayer/plaid-penguin)
- [lein-thrift](https://github.com/kurtharriger/lein-thrift)

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
