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
  2: Location location
}

struct People {
  1: set<Person> people
}
