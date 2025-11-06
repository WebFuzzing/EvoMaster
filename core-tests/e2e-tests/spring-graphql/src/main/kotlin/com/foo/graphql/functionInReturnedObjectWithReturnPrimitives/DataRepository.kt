package com.foo.graphql.functionInReturnedObjectWithReturnPrimitives

import com.foo.graphql.functionInReturnedObjectWithReturnPrimitives.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower {
        return flowers
    }



}




