namespace java com.foo.rcp.example


service ThriftExampleService {

    bool: A boolean value (true or false)
    byte: An 8-bit signed integer
    i16: A 16-bit signed integer
    i32: A 32-bit signed integer
    i64: A 64-bit signed integer
    double: A 64-bit floating point number
    string: A text string encoded using UTF-8 encoding

    bool baseTypeEndpoint(1:bool arg1, 2:byte arg2, 3:i16 arg3, 4:i64 arg4, 5:double arg5, 6:string arg6),
}