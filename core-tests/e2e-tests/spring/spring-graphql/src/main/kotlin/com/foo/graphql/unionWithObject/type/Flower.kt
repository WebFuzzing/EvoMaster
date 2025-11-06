package com.foo.graphql.unionWithObject.type


data class Flower(
    override var id: Int,
    var name: String,
    var color: String,
    var price: Int
) : Store{


    fun name(): Name {

        return Name("Roses","Tulips")

    }

}

