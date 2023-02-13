package com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself


import com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower? {
        return flowers
    }

}




