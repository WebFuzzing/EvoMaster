package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2

import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower {
        return flowers
    }

}




