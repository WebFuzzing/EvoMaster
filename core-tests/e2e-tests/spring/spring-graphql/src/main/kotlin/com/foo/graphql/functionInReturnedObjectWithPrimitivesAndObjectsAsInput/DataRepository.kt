package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput

import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower {
        return flowers
    }

}




