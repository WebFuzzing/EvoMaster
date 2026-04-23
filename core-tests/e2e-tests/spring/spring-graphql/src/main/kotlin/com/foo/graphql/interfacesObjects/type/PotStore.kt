package com.foo.graphql.interfacesObjects.type


class PotStore (
        override val id: Int? = null,
        override val name: String? = null,
        val address: Address? = null
): Bouquet{

fun  address() : Address? {
        return AddressFlower(20,"nameFlower20","street20")}
}
