package com.foo.graphql.enumInput

import com.foo.graphql.enumInput.type.FlowerType
import com.foo.graphql.enumInput.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = mutableMapOf<FlowerType, Flower>()

    init {
        listOf(Flower(0, "Darcey", FlowerType.LILIES, "Red", 50),
                Flower(1, "Candy Prince", FlowerType.LIMONIUM, "Pink", 18),
                Flower(2, "Lily", FlowerType.ROSES, "White", 30),
                Flower(3, "Lavender", FlowerType.TULIPS, "Purple", 25)
        )

    }


    fun findByFlTy(type: FlowerType?): Flower? {
        return flowers[type]

    }

}

