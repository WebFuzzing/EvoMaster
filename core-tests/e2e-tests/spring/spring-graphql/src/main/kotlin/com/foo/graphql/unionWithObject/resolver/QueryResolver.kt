package com.foo.graphql.unionWithObject.resolver


import com.foo.graphql.unionWithObject.type.Store
import com.foo.graphql.unionWithObject.DataRepository
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver{

    fun stores(): List<Store> {
        return dataRepo.getStores()
    }


}
