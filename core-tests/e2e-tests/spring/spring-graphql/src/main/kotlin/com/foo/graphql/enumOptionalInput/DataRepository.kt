package com.foo.graphql.enumOptionalInput


import com.foo.graphql.enumOptionalInput.type.Flower
import com.foo.graphql.enumOptionalInput.type.FlowerType
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


    fun findByFlTy(type: FlowerType?): Flower? {
        return if(type==null){
            Flower(null, "NullFlower", FlowerType.LILIES , "null", null)
        }else {
            flowers[type]
        }
    }

}

