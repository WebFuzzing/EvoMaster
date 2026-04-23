package com.foo.graphql.input

import com.foo.graphql.input.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = mutableMapOf<Int?, Flower>()


    init {
        listOf(Flower(0, "Darcey", "Roses", "Red", 50),
                Flower(1, "Candy Prince", "Tulips", "Pink", 18),
                Flower(2, "Lily", "Lilies", "White", 30),
                Flower(3, "Lavender", "Limonium", "Purple", 25)
        ).forEach { flowers[it.id] = it }

    }


    fun findById(id: Int): Flower? {
        return flowers[id]
    }

}




