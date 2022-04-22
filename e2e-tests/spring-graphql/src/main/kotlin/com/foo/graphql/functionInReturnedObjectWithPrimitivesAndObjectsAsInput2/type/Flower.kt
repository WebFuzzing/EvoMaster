package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.type


class Flower (
     var type: String =""
    ) {

    fun type(id: Int?, store : Store?): String {

        if(id == null){
            return "NULL_ID"
        }

        if(store != null){
            return "NON_NULL_STORE"
        }
        return "FOO"
    }

}