package com.foo.graphql.nullable.array

import com.foo.graphql.nullable.array.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    fun findFlowers(id: Array<Int?>?): Flower? {
        return if (id == null) null //for Array<X>?
        else
            if (id.all { it != null }) Flower(0, "flowerNameX") else null
    }

}




