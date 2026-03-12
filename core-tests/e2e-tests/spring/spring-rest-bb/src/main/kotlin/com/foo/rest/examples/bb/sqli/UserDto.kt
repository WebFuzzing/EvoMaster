package com.foo.rest.examples.bb.sqli

// TODO: Currently unused. May be needed if BB tests can run with Docker in GitHub Actions.

data class UserDto(
    var id: Long? = null,
    var username: String? = null,
)

data class LoginDto(
    var username: String,
    var password: String
)
