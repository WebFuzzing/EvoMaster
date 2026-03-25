package com.foo.spring.rest.postgres.sqli.path


data class UserDto(
    var id: Long? = null,
    var username: String? = null,
)

data class LoginDto(
    var username: String = "",
    var password: String = ""
)
