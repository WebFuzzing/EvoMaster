package com.foo.graphql.tupleWithOptLimitInLast.type


class Flower(
    var price:Int?=null
) {


    fun price(id: Int?): Store? {
        return findPrice(id)
    }


    private fun findPrice(id: Int?): Store? {

        val store = Store()
        when (id) {
            null ->{
                store.name.plus("Null store")
                store.id = 0
                return store
            }

            1 -> {
                store.name.plus("Roses store")
                store.id = 11
                return store
            }
            2 -> {
                store.name.plus("Tulips store")
                store.id = 22
                return store
            }
            3 -> {
                store.name.plus("Lilies store")
                store.id = 33
                return store
            }
            4 -> {
                store.name.plus(" Limonium store ")
                store.id = 44
                return store
            }
        }
        store.id = 0
        store.name.plus("At least it is a flower in a flower store !!!")
        return store
    }
}