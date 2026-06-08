package com.foo.rest.examples.spring.openapi.v3.logincreateuser

class CreateUserDto(
    var email: String? = null,
    var password: String? = null,
    var repeatPassword: String? = null,
    var username: String? = null
)
