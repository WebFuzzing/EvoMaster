package com.foo.graphql.cycleInTuple.type

class Store(
    var id: Int? = null,
    var name: Flower? = null
) {
    fun name(id: Int?): Flower {
        when (id) {
            null-> return Flower("null",0)
            1 -> return Flower("Roses",10)
            2 -> return Flower("Tulips",20)
            3 -> return Flower("Lilies",30)
            4 -> return Flower( " Limonium",40)
        }
        return Flower("At least it is a flower in a flower store !!!",100)
    }
}