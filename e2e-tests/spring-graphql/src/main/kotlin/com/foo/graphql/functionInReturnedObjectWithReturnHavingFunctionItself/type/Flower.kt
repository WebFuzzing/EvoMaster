package com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself.type


class Flower(
    var type: String = ""
) {

    fun type(id: Int): Store {
        return findType(id)
    }

    private fun findType(id: Int): Store {

        val store = Store()
        when (id) {
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