namespace java org.example

struct Name {
  1: optional string firstName,
  2: string lastName
}

struct Person {
  1: i32 id
  2: Name name,
  3: byte age
}

service PersonIndex {
    void store(1:Person p),
    Person getPerson(1:i32 id)
}
