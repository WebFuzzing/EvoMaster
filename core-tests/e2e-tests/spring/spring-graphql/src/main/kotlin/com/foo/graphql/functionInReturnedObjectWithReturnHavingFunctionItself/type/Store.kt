package com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself.type

class Store(
    var id: Int? = null,
    var name: String? = null
) {
    fun name(id: Int?): String {
        when (id) {
            null-> return "Null store"
            1 -> return "Roses store"
            2 -> return "Tulips store"
            3 -> return "Lilies store"
            4 -> return " Limonium store "
        }
        return "At least it is a flower in a flower store !!!"
    }
}