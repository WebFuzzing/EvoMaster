package com.foo.graphql.optionalPrimitiveArrayInput

import com.foo.graphql.optionalPrimitiveArrayInput.type.Flower
import org.springframework.stereotype.Component



@Component
open class DataRepository {


    fun findById(id: Array<Int?>?): Flower? {
       return when {
            (id == null) -> Flower(0, "Darcey", "Roses", "Red", 50)
            (id.any { it == null }) -> Flower(1, "Candy Prince", "Tulips", "Pink", 18)
            else ->  Flower(2, "Lily", "Lilies", "White", 30)
        }
    }


}




