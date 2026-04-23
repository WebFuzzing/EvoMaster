package com.foo.graphql.enumInput.type

data class Flower (
        var id: Int? = null,
        var name: String? = null,
        var type: FlowerType,
        var color: String? = null,
        var price: Int? = null)