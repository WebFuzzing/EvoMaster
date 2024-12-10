package com.foo.graphql.cycleInTuple.type



class Flower(
    var type: String?=null,
) {
    fun type(id: Int?): Store {
        return findType(id)
    }

    private fun findType(id: Int?): Store {

        val store = Store()
        when (id) {
            null ->{
                store.name= Flower("Null")
                store.id = 0
                return store
            }
            1 -> {
                store.name=Flower("Roses")
                store.id = 11
                return store
            }
            2 -> {
                store.name=Flower("Tulips")
                store.id = 22
                return store
            }
            3 -> {
                store.name=Flower("Lilies")
                store.id = 33
                return store
            }
            4 -> {
                store.name=Flower(" Limonium")
                store.id = 44
                return store
            }
        }
        store.id = 0
        store.name=Flower("At least it is a flower in a flower store !!!")
        return store
    }

}
