package com.foo.graphql.enumIntupleArg.type

class Store(
    var id: Int? = null,
    var name: StoreType? = null
) {
    fun name(id: StoreType?): String {
        return when (id) {
            null-> "Null store"
            StoreType.STOREA -> "A store"
            StoreType.STOREB -> "B store"
        }

    }
}