package com.foo.graphql.unionWithInput


import com.foo.graphql.unionWithInput.type.Flower
import com.foo.graphql.unionWithInput.type.Pot
import com.foo.graphql.unionWithInput.type.Store
import org.springframework.stereotype.Component

@Component
open class DataRepository {

    fun getStores(id:Int): List<Store> {
        return listOf(
                Flower(0, "Calandiva", "pink",70),
                Pot(1, "pot1", 48)
        )
    }

}