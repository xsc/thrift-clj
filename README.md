# thrift-clj

A Clojure library designed to ... well, that part is up to you.

## Example

__test.thrift__

```thrift
namespace java org.example

struct Person {
  1: string firstName,
  2: optional string lastName,
  3: byte age
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
(use '[thrift-clj.core :exclude [load]])
(thrift/load "org.example")
```

This will load all types that reside in a package _prefixed by_ "org.example". Clojure types
of the same name will be created in the current namespace. (In future releases, `require`-like
specifications with `:only` or `:exclude` should be possible. As of now, loading of a single type
is not supported.)

```clojure
(def some-person (Person. "Some" "One" (byte 24)))
(pprint some-person) ;; => {:firstName "Some", :lastName "One", :age 24}

(def some-thrift-person (clj->thrift some-person))
(pprint some-thrift-person) ;; => #<Person Person(firstName:Some, lastName:One, age:24)>
``` 

Now, that's all I got so far. As you can see there is no automatic conversion from `int`/`long`
to `byte`, so we have to manually cast the number. To facilitate this is on the TODO list as well.

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
