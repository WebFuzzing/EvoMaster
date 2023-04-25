package com.foo.graphql.cycleInTuple.type

class Store(
    var id: Int? = null,
    var name: Flower? = null
) {
    fun name(id: Int?): Flower {
        when (id) {
            null-> return Flower("null")
            1 -> return Flower("Roses")
            2 -> return Flower("Tulips")
            3 -> return Flower("Lilies")
            4 -> return Flower( "Limonium")
        }
        return Flower("At least it is a flower in a flower store !!!")
    }
}