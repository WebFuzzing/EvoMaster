package com.foo.graphql.functionInReturnedObject

import com.foo.graphql.functionInReturnedObject.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower {
        return flowers
    }



}




