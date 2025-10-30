package com.foo.graphql.unionFunctions.type


data class Flower(
    override var id: Int? = null,
    var name: String? = null,
    var color: String? = null,
    var price: Int? = null
) : Store {


    fun name(x: Int): String {

        when (x) {
            1 -> return "Roses"
            2 -> return "Tulips"
            3 -> return "Lilies"
            4 -> return "Limonium"
        }
        return "At least it is a flower!!!"
    }
}

