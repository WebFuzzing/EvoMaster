package com.foo.graphql.nullableNonNullableInReturn

import com.foo.graphql.nullableNonNullableInReturn.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {


    fun flowerN(): Flower? {
    //BUG here, because id is declared as a non nullable field in the schema
       return Flower(null, "Darcey")
    }

}
