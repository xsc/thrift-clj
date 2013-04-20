# thrift-clj

[![Build Status](https://travis-ci.org/xsc/thrift-clj.png?branch=master)](https://travis-ci.org/xsc/thrift-clj)

Using Thrift from Clojure as if it was Clojure.

__Note__: This is an alpha release. The API might change at any point (but, at least for the most prominent parts, 
probably won't).

## Usage

thrift-clj is available via [Clojars](http://clojars.org/thrift-clj).

__Leiningen__

```clojure
[thrift-clj "0.1.0-alpha7"]
```

__Note__: Tested with the Thrift 0.9.0 compiler. Since this depends massively on the generated code, make sure to use
that version (or any other one that was tested with this library).

__Automatic Thrift Compilation__

I recommend [lein-thriftc](https://github.com/xsc/lein-thriftc) for automatic compilation of Thrift IDL files to Java
class files.

## Example

A working example demonstrating Service and Client implementation should always be available as 
[thrift-clj-example](https://github.com/xsc/thrift-clj-example). A small peek follows.

### Accessing Types

__Thrift__

```thrift
namespace java org.example

struct Person {
  1: optional string firstName,
  2: string lastName,
  3: byte age
}
```

Compile to Java using Thrift and add to Leiningen's classpath. (see `:java-source-paths`)

__Clojure__

```clojure
(require '[thrift-clj.core :as thrift])
(thrift/import 
  (:types [org.example Person]))
  
(def clj-p (Person. "Some" "One" 99)) 
;; => #ns_1071852349.Person{:firstName "Some", :lastName "One", :age 99}

(def thr-p (thrift/->thrift clj-p))   
;; => #<Person Person(firstName:Some, lastName:One, age:99)>

(class clj-p) ;; => ns_1071852349.Person
(class thr-p) ;; => org.example.Person
```

### Implementing a Service

__Thrift__

```thrift
namespace java org.example

// ... Person struct from above ...

service PersonIndex {
    bool storePerson(1:i32 id, 2:Person p),
    Person getPerson(1:i32 id)
}
```

__Clojure__

```clojure
(require '[thrift-clj.core :as thrift])
(thrift/import 
  (:types [org.example Person])
  (:services org.example.PersonIndex))

(defonce person-db (atom {}))
(thrift/defservice person-index-service
  PersonIndex
  (storePerson [id p]
    (boolean
      (when-not (@person-db id)
        (info "Storing Person:" p)
        (swap! person-db assoc id p)
        true)))
  (getPerson [id]
    (info "Retrieving Person for ID:" id)
    (@person-db id))) 
    
(thrift/serve-and-block!
  (thrift/multi-threaded-server
    person-index-service 7007
    :bind "localhost"
    :protocol :compact))
```

### Running a Client

```clojure
(require '[thrift-clj.core :as thrift])
(thrift/import 
  (:types [org.example Person])
  (:clients org.example.PersonIndex))
  
(with-open [c (thrift/connect! PersonIndex ["localhost" 7007])]
  (PersonIndex/storePerson c 1 (Person. "Some" "One" 99))
  (PersonIndex/getPerson c 1))
```

## Roadmap

- ~~Have a look at Lists/Sets/Enums/...~~
- ~~wrappers around Protocols (to encode/decode values directly to/from byte arrays)~~
- ~~more protocols/servers/clients~~
- asynchronous client/server
- union?
- tests & documentation
- ...

## Related Work/Inspiration

- [Apache Thrift](https://github.com/apache/thrift)
- [Plaid Penguin](https://github.com/ithayer/plaid-penguin)
- [lein-thriftc](https://github.com/xsc/lein-thriftc)
- [thrift-clj-example](https://github.com/xsc/thrift-clj-example)

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
