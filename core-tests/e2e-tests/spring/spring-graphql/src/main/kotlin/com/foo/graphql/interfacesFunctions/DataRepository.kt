package com.foo.graphql.interfacesFunctions


import com.foo.graphql.interfacesFunctions.type.FlowerStore
import com.foo.graphql.interfacesFunctions.type.PotStore
import com.foo.graphql.interfacesFunctions.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {


    fun getStores(): List<Store> {
        return listOf(
                FlowerStore(0, "flowerStore0"),
                PotStore(1, "potStore1", "BlimBlim")
        )
    }


}