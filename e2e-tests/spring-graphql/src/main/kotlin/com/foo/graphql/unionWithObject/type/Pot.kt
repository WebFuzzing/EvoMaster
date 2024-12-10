package com.foo.graphql.unionWithObject.type


data class Pot(
        override var id: Int,
        val color: String,
        val size: Int
) : Store
