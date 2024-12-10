namespace java com.foo.rpc.examples.spring.fakemockobject.generated

struct FakeRetrieveData{

    1: required i32 id,
    2: required string name,
    3: required string info
}

struct FakeDatabaseRow{

    1: required i32 id,
    2: required string name,
    3: required string info
}

service FakeMockObjectService {

    string getFooFromExternalService(1:i32 id),

    string getBarFromDatabase(1:i32 id),

    list<string> getAllBarFromDatabase(),

    bool backdoor(1:FakeRetrieveData exData, 2:FakeDatabaseRow dbData)

}
