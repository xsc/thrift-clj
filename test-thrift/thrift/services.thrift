namespace java thriftclj.services
include "structs.thrift"

service TelephoneBook {
  bool storePerson(1:structs.Person p),
  set<structs.Person> findByName(1:optional string firstName, string lastName),
  set<structs.Person> findByLocation(1:structs.Location loc)
} 
