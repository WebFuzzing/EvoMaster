package com.foo.graphql.unionFunctions.resolver


import com.foo.graphql.unionFunctions.type.Store
import com.foo.graphql.unionFunctions.DataRepository
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
