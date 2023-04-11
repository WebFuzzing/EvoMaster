package com.foo.graphql.enumIntupleArg.type


class Flower(
    var type: FlowerType?=null,
) {

    fun type(id: FlowerType?): Store? {
        return findType(id)
    }

    private fun findType(id: FlowerType?): Store {

        val store = Store()
        when (id) {
            null ->{
                store.name= null
                store.id = 0
                return store
            }
            FlowerType.ROSES -> {
                store.name= arrayOf(StoreType.STOREA)
                store.id = 11
                return store
            }
            FlowerType.TULIPS -> {
                store.name= arrayOf(StoreType.STOREB)
                store.id = 22
                return store
            }
        }
    }

}