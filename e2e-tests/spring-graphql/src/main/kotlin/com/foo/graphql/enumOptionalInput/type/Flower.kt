package com.foo.graphql.enumOptionalInput.type

import com.foo.graphql.enumOptionalInput.type.FlowerType

data class Flower (
        var id: Int? = null,
        var name: String? = null,
        var type: FlowerType,
        var color: String? = null,
        var price: Int? = null)