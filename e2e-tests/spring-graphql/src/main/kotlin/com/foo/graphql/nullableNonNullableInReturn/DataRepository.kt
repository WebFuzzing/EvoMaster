package com.foo.graphql.nullableNonNullableInReturn

import com.foo.graphql.nullableNonNullableInReturn.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {


    fun flowerN(): Flower? {

       return Flower(null, "Darcey")
    }

}
