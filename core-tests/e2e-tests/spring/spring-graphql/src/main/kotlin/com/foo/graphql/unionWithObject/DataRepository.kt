package com.foo.graphql.unionWithObject

import com.foo.graphql.unionWithObject.type.Store
import com.foo.graphql.unionWithObject.type.Flower
import com.foo.graphql.unionWithObject.type.Pot
import org.springframework.stereotype.Component

@Component
open class DataRepository {

    fun getStores(): List<Store> {
        return listOf(
                Flower(0, "Calandiva", "pink",70),
                Pot(1, "pot1", 48)
        )
    }

}