package com.foo.graphql.nullableNonNullableInInput.type

data class Flower (
     var id: Int? = null,
     var name: String? = null,
     var type: String? = null,
     var color: String? = null,
     var price: Int? = null)