package com.foo.graphql.enum

import com.foo.graphql.enum.type.FlowerType
import com.foo.graphql.enum.type.Flower
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
open class DataRepository {

    private val flowers = mutableMapOf<Int?, Flower>()

    private val counter = AtomicInteger(0)

    init {
        listOf(Flower(0, "Darcey", FlowerType.ROSES, "Red", 50),
                Flower(1, "Candy Prince", FlowerType.LILIES, "Pink", 18),
                Flower(2, "Lily", FlowerType.TULIPS, "White", 30),
                Flower(3, "Lavender", FlowerType.LIMONIUM, "Purple", 25)
        ).forEach { flowers[it.id] = it }

    }


    fun allFlowers(): Collection<Flower> {
        return flowers.values
    }
}