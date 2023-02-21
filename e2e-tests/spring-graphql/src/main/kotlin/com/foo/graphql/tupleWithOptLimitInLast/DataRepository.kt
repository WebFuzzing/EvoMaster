package com.foo.graphql.tupleWithOptLimitInLast


import com.foo.graphql.tupleWithOptLimitInLast.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower? {
        return flowers
    }

}




