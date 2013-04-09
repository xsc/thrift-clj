namespace java org.thriftclj.example

struct Name {
    1: optional string firstName,
    2: string lastName
}

struct Person {
    1: Name name,
    2: byte age
}

service TestServer {
    i32 storePerson(1:Person p)
}
