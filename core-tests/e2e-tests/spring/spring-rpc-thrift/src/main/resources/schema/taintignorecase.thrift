namespace java com.foo.rpc.examples.spring.taintignorecase

service TaintIgnoreCaseService{
    string getIgnoreCase(1: required string value)
}