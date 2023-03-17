package com.foo.graphql.enumIntupleArg.type

class Store(
    var id: Int? = null,
    var name: Array<StoreType?>? = null
) {
    fun name(id: Array<StoreType?>?): String {
        return when (id) {
            null-> "Null store"
            arrayOf(StoreType.STOREA)->"A store"
            arrayOf(StoreType.STOREB)->"B store"
            else -> ""
        }

    }
}