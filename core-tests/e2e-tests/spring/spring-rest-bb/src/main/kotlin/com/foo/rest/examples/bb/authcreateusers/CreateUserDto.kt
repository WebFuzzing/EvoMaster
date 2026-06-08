package com.foo.rest.examples.bb.authcreateusers

class CreateUserDto(
    var email: String? = null,
    var password: String? = null,
    var repeatPassword: String? = null,
    var username: String? = null
)
