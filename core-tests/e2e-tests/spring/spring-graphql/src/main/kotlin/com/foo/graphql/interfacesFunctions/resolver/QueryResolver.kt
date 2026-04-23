package com.foo.graphql.interfacesFunctions.resolver


import com.foo.graphql.interfacesFunctions.type.Store
import com.foo.graphql.interfacesFunctions.DataRepository
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver {

    fun stores(): List<Store> {
        return dataRepo.getStores()
    }

}
