namespace java com.foo.rpc.examples.spring.thriftexception

exception BadResponse {
    1: i32 code,
    2: string message
}

exception ErrorResponse {
    1: i32 code,
    2: string message
}

service ThriftExceptionService {

    string check(1:string value) throws (1:BadResponse bad, 2:ErrorResponse error)
}