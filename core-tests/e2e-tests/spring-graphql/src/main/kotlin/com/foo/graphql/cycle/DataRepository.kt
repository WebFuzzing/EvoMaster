package com.foo.graphql.cycle

import com.foo.graphql.cycle.type.Bouquet
import com.foo.graphql.cycle.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    private val bouquets = mutableMapOf<String, Bouquet>()
    private val stores = mutableMapOf<String, Store>()

    init {
        listOf(Bouquet("0", "A", "s", "20"),
                Bouquet("1", "B", "m", "21"),
                Bouquet("2", "C", "l", "22"),
                Bouquet("3", "D", "xl", "23")
        ).forEach { bouquets[it.id] = it }

        listOf(Store("0", "A1", "55"),
                Store("1", "B2", "32"),
                Store("2", "C3", "23"),
                Store("3", "D4", "15")
        ).forEach { stores[it.id] = it }
    }

    fun allBouquets(): Collection<Bouquet> {
        return bouquets.values
    }

    fun findStoreById(id: String): Store? {
       return stores[id]
    }

    fun findBouquetById(id: String): Bouquet? {
        return bouquets[id]
    }

}




