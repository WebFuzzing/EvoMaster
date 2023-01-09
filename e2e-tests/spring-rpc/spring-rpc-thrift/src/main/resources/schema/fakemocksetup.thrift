namespace java com.foo.rpc.examples.spring.fakemocksetup

struct UserDto {
    1: required string id,
    2: required string passcode
}

service FakeMockSetupService {

    string accessValidation(1: UserDto dto)
}