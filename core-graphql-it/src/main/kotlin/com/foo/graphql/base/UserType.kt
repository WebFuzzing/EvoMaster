package com.foo.graphql.base


data class UserType(
        var id: String,
        var name: String?,
        var surname: String?,
        var age: Int?
)