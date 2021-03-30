package com.foo.graphql.nullableNonNullableInReturn

import com.foo.graphql.nullableNonNullableInReturn.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {


    fun flower(): Flower {

       return Flower(0, null, "Roses", "Red", 50)
    }


}
