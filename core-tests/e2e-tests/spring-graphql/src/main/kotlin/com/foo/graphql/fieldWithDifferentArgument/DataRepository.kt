package com.foo.graphql.fieldWithDifferentArgument

import com.foo.graphql.fieldWithDifferentArgument.type.Flower
import com.foo.graphql.fieldWithDifferentArgument.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val flowers = mutableMapOf<Int, Flower>()
    private val stores = mutableMapOf<Int, Store>()

    init {
        listOf(
            Flower(0,  "Roses", "Red"),
                Flower(1,  "Tulips", "Pink"),
                Flower(2,  "Lilies", "White"),
                Flower(3,  "Limonium", "Purple")
        ).forEach { flowers[it.id] = it }

        listOf(
            Store(0,  "Roses" , 5),
            Store(1,  "Tulips",9),

        ).forEach { stores[it.id] = it }

    }

    fun findByIdAndColor(id: Int?, color: String?): Flower {

        if (id != null || color != null) {

            for (flower in flowers) {
                if (flower.value.id == id)
                    if (flower.value.color == color)
                        return flower.value
            }
            return Flower(100, "X", "Color:X")
        } else return Flower(100, "X", "Color:X")
    }


    fun findById(id: Int): Flower {
        return flowers[id]!!
    }

    fun findStores():Store{
        return stores[0]!!
    }

}




