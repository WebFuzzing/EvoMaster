package com.foo.graphql.unionWithInput.resolver



import com.foo.graphql.unionWithInput.DataRepository
import com.foo.graphql.unionWithInput.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver{

    fun stores(id:Int): List<Store> {
        return dataRepo.getStores(id)
    }


}
