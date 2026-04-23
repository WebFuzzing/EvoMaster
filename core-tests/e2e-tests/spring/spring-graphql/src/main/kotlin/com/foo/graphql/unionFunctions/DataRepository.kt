package com.foo.graphql.unionFunctions

import com.foo.graphql.unionFunctions.type.Store
import com.foo.graphql.unionFunctions.type.Flower
import com.foo.graphql.unionFunctions.type.Pot
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