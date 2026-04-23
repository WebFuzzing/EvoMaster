namespace java com.foo.rpc.examples.spring.testability

service TestabilityService{

    string getSeparated(1: required string date, 2: required string number, 3: required string setting)
}