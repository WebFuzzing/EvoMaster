namespace java com.foo.rpc.examples.spring.authsetup

struct LoginDto {
    1: required string id,
    2: required string passcode
}

service AuthSetupService {

    string access(),

    void login(1:LoginDto dto)

    void logout()

}