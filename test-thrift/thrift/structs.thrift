namespace java thriftclj.structs

struct Name {
  1: optional string firstName,
  2: string lastName
}

enum Country {
  DE
  AT
  US
  GB
}

struct Location {
  1: i16 zip,
  2: string city,
  3: Country country
}

struct Person {
  1: Name name,
  2: optional Location location,
  3: bool living
}

struct People {
  1: optional set<Person> peopleSet,
  2: optional list<Person> peopleList,
  3: optional map<i32,Person> peopleMap
}
