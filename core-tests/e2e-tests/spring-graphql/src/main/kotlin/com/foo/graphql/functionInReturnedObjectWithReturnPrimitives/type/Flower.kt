package com.foo.graphql.functionInReturnedObjectWithReturnPrimitives.type


class Flower (
     var type: String =""
    ) {

    fun type(id: Int): String {
        return findType(id)
    }
    private fun findType(id:Int):String{

        when (id) {
            1 -> return "Roses"
            2 -> return "Tulips"
            3 -> return "Lilies"
            4 -> return "Limonium"
        }
        return "At least it is a flower!!!"
    }
}