package com.foo.graphql.interfacesFunctions.type

data class FlowerStore(
        override val id: Int? = null,
        override val name: String? = null
) : Store
{
        override fun name(x: Int): String {
                return findNameByX(x)
        }

        override fun findNameByX(x: Int): String {

                when (x) {
                        1 -> return "FlowerStore1"
                        2 -> return "FlowerStore2"
                        3 -> return "FlowerStore3"
                        4 -> return "FlowerStore4"
                }
                return "It is a Flower store x"
        }
}
