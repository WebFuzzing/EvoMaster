namespace java com.foo.rpc.examples.spring.nullableparam

struct NullableDto {
    1: optional i32 value = 42
    2: optional i32 novalue
}

struct RequiredDto {
    1: required i32 value
}

service NullableParamService {

    void handleAnyNullable(1:NullableDto dto),

    void handleAnyRequired(1:RequiredDto dto),

    void handleRequiredNullable(1:required NullableDto dto),

    void handleRequiredRequired(1:required RequiredDto dto)
}