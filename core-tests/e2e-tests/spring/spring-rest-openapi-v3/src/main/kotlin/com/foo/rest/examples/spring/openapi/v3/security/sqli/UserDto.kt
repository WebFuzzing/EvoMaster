package com.foo.rest.examples.spring.openapi.v3.security.sqli


data class UserDto(
    var id: Long? = null,
    var username: String? = null,
)

data class LoginDto(
    var username: String,
    var password: String
)
