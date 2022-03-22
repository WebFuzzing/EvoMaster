package com.foo.graphql.unionFunctions.type



data class Pot(
        override var id: Int? = null,
        val color: String? = null,
        val size: Int? = null
) : Store

{
        fun color(y: Int): String {
                when (y) {
                        1 -> return "Pot01"
                        2 -> return "Pot02"
                        3 -> return "Pot03"
                        4 -> return "Pot04 "
                }
                return "At least it is a Pot !!!"
        }
}