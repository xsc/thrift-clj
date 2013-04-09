namespace java org.thriftclj.example

struct Person {
    1: string firstName,
    2: optional string lastName,
    3: byte age
}

service TestServer {
    i32 storePerson(1:Person p)
}
