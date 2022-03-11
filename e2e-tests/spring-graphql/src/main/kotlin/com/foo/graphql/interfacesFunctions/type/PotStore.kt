package com.foo.graphql.interfacesFunctions.type

data class PotStore (
        override val id: Int? = null,
        override val name: String? = null,
        val address: String? = null
): Store {
        override fun name(x: Int): String {
                return findNameByX(x)
        }

        override fun findNameByX(x: Int): String {

                when (x) {
                        5 -> return "PotStore1"
                        6 -> return "PotStore2"
                        7 -> return "PotStore3"
                        8 -> return "PotStore4"
                }
                return "It is a Pot store x"
        }
}