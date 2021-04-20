package com.foo.graphql.cycle.resolver

import com.foo.graphql.cycle.DataRepository
import com.foo.graphql.cycle.type.Bouquet
import com.foo.graphql.cycle.type.Store
import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class BouquetResolver (private val dataRepo: DataRepository): GraphQLResolver<Bouquet> {

    fun getId(bouquet: Bouquet) = bouquet.id
    fun getName(bouquet: Bouquet) = bouquet.name
    fun getPot(bouquet: Bouquet) = bouquet.pot
    fun getStore(bouquet: Bouquet): Store {
        return dataRepo.findStoreById(bouquet.storeId)
        // FIXME: not really realistic, but it is just a test
                ?: Store(bouquet.id, "foo", bouquet.id)
    }


}
