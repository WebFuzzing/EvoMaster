package com.foo.graphql.mutationObject

import com.foo.graphql.mutationObject.type.Flower
import com.foo.graphql.mutationObject.type.Price
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger


@Component
open class DataRepository {

    private val flowers = mutableMapOf<Int?, Flower>()

    private val counter = AtomicInteger(0)

    init {
        listOf(
            Flower(10,  "Rose"),
                Flower(20,  "Tulip"),
                Flower(30,  "Lilies"),
                Flower(40,  "Limonium")
        ).forEach { flowers[it.id] = it }

    }

   fun allFlowers(): Collection<Flower?>? = flowers.values

    fun saveFlower(price: Price?): Flower? {
        if (price == null || price.month == null || price.year == null) return Flower(0, "Null") else
            return Flower(10, "Rose")

    }

}




