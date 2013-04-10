namespace java org.example

struct Person {
  1: optional string firstName,
  2: string lastName,
  3: byte age
}

service PersonIndex {
    bool store(1:Person p)
}
