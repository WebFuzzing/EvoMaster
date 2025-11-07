package com.foo.graphql.arrayEnumInput


import com.foo.graphql.arrayEnumInput.type.Flower
import com.foo.graphql.arrayEnumInput.type.FlowerType
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = mutableMapOf<FlowerType, Flower>()

    init {
        listOf(Flower(0, "Darcey", FlowerType.LILIES, "Red", 50),
                Flower(1, "Candy Prince", FlowerType.LIMONIUM, "Pink", 18),
                Flower(2, "Lily", FlowerType.ROSES, "White", 30),
                Flower(3, "Lavender", FlowerType.TULIPS, "Purple", 25)
        ).forEach { flowers[it.type] = it }

    }


    fun findByFlTy(type: Array< FlowerType>): Flower? {

        return if (type.isEmpty()) flowers[FlowerType.TULIPS]
        else flowers[type.get(0)]
    }

}

