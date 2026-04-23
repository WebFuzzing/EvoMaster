package com.foo.spring.rest.mysql.sqli.body

import com.fasterxml.jackson.annotation.JsonProperty

data class UserDto(
    @JsonProperty("id") var id: Long? = null,
    @JsonProperty("username") var username: String? = null,
)

data class LoginDto(
    @JsonProperty("username") var username: String = "",
    @JsonProperty("password") var password: String = ""
)
