namespace java com.foo.rpc.examples.spring.taint

service TaintService{

    string getInteger(1: required string value),

    string getDate(1: required string value),

    string getConstant(1: required string value),

    string getThirdParty(1: required string value),

    string getCollection(1: required string value)
}