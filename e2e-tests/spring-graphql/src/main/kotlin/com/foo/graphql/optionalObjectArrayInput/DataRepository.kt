package com.foo.graphql.optionalObjectArrayInput

import com.foo.graphql.optionalObjectArrayInput.type.Flower
import com.foo.graphql.optionalObjectArrayInput.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    fun findFlower(store: Array<Store?>?): Flower? {
        return when {
            (store == null) -> Flower(0, 50)
            (store.any { it == null }) -> Flower(1,18)
            else -> Flower(2,  30)
        }

    }

}




