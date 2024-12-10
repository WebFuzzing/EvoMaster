package com.foo.graphql.enumIntupleArg


import com.foo.graphql.enumIntupleArg.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower? {
        return flowers
    }

}




