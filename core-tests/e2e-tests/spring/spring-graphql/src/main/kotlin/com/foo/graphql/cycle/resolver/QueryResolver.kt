package com.foo.graphql.cycle.resolver

import com.foo.graphql.cycle.DataRepository
import com.foo.graphql.cycle.type.Bouquet
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {

    fun bouquets(): List<Bouquet>{
        return  dataRepo.allBouquets().toList()
    }



}


