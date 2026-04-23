package com.foo.graphql.cycle.resolver

import com.foo.graphql.cycle.DataRepository
import com.foo.graphql.cycle.type.Bouquet
import com.foo.graphql.cycle.type.Store
import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class StoreResolver(private val dataRepo: DataRepository): GraphQLResolver<Store> {

    fun getId(store: Store) = store.id
    fun getAdress(store: Store) = store.address
    fun getBouquet(store: Store): Bouquet? {
        return dataRepo.findBouquetById(store.bouquetId)
    }



}