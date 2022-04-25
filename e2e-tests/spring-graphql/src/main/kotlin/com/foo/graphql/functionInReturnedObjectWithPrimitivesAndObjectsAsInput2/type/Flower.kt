package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.type


class Flower (
     var type: String =""
    ) {

    fun type(id: Int?, store : Store?): String {

        if(id == null){
            if(store == null){
                return "BOTH_NULL"
            }

            return "NULL_ID"
        }

        if(store != null){
            return "NON_NULL_STORE"
        }
        return "FOO"
    }

}