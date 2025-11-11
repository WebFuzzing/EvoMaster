namespace java com.foo.rpc.examples.spring.numericstring

struct StringDto {
    1: string longValue,
    2: string longValue,
    3: string doubleValue
}

service NumericStringService{

    string getNumber(1: StringDto value)
}