package com.foo.graphql.tupleWithOptLimitInLast.type

class Store(
    var id: Int? = null,
    var name: String? = null
) {
    fun name(id: Int?): Address? {
        when (id) {
            //re-do
            null-> return Address(0,"StreetName 0")
            1 -> return Address(1,"StreetName 1 ")
            2 -> return Address(2,"StreetName 2")
            3 -> return Address(3,"StreetName 3")
            4 -> return Address(4,"StreetName 4")
        }
        return Address(100,"StreetName 100")
    }
}