package com.foo.graphql.interfacesObjects.resolver


import com.foo.graphql.interfacesObjects.DataRepository
import com.foo.graphql.interfacesObjects.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver {



    fun stores(): Store {
        return dataRepo.getStores()
    }
}
