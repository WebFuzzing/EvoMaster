package com.foo.graphql.interfacesObjects.type

data class AddressStore (
        override val id: Int? = null,
        override val street: String? = null,
        val nameStore: String? = null
): Address
