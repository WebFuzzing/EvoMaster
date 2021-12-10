package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput.type


class Flower (
     var type: String =""
    ) {

    fun type(id: Int, store :Store): String {
        return findTypeByIdAndStore(id, store)
    }
    private fun findTypeByIdAndStore(id:Int, store:Store):String{

        when (id) {
            1 -> if (store.id == 1)return "Roses"
            2 -> if (store.id == 2)return "Tulips"
            3 -> if (store.id == 3)return "Lilies"
            4 -> if (store.id == 4)return "Limonium"
        }
        return "At least it is a flower in a random store !!!"
    }
}