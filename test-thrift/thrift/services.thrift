namespace java thriftclj.services
include "structs.thrift"
include "exceptions.thrift"

service TelephoneBook {
  bool storePerson(1:structs.Person p) throws (1:exceptions.StorageError e),
  set<structs.Person> findByName(1:optional string firstName, string lastName),
  set<structs.Person> findByLocation(1:structs.Location loc)
}
