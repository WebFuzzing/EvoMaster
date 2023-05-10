package com.foo.graphql.cycleInTuple



import com.foo.graphql.cycleInTuple.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = Flower()

    fun findFlower(): Flower {
        return flowers
    }
}




