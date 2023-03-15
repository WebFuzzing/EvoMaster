package com.foo.graphql.interfacesObjects




import com.foo.graphql.interfacesObjects.type.*

import org.springframework.stereotype.Component


@Component
open class DataRepository {
    fun getStores(): Store {
        return Store(listOf(FlowerStore(5, "Calandiva"), PotStore(10, "Green", AddressFlower(15,"nameFlower15","street15"))))

    }
}