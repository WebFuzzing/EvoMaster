package com.foo.graphql.interfacesObjects.type

data class FlowerStore(
        override val id: Int? = null,
        override val name: String? = null
) : Bouquet
