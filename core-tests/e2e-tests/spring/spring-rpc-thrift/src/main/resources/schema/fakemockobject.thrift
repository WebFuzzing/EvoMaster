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

enum FakeScheduleTaskState {
  NOT_START = 1,
  PROCESSING = 2,
  COMPLETED = 3
}

struct FakeScheduleTaskData{

    1: required i64 id,
    2: required string name,
    3: required string startTime,
    4: required string info,
    5: required FakeScheduleTaskState state
}

service FakeMockObjectService {

    string getFooFromExternalService(1:i32 id),

    string getBarFromDatabase(1:i32 id),

    bool isExecutedToday(),

    list<string> getAllBarFromDatabase(),

    bool backdoor(1:FakeRetrieveData exData, 2:FakeDatabaseRow dbData, 3:FakeScheduleTaskData scheduleTask)

}
