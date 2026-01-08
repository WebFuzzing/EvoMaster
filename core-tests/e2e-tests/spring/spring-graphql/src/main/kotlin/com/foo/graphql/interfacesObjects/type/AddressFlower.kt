package com.foo.graphql.interfacesObjects.type

data class AddressFlower(
        override val id: Int? = null,
        override val street: String? = null,
        val nameFlower: String? = null,

) : Address
