package com.foo.graphql.inputObject

import com.foo.graphql.inputObject.type.Flower
import com.foo.graphql.inputObject.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    fun findFlower(store: Store): Flower? {
            return Flower(4, 88)
    }

}




