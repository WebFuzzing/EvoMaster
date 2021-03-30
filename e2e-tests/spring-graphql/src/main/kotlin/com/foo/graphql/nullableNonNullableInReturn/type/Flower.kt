package com.foo.graphql.nullableNonNullableInReturn.type

data class Flower(
        var id: Int,
        var name: String?,
        var type: String? = null,
        var color: String? = null,
        var price: Int? = null)