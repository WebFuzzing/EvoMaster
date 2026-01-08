package com.foo.graphql.cycleInTuple.resolver

import com.foo.graphql.cycleInTuple.DataRepository
import com.foo.graphql.cycleInTuple.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {

    fun flowers(): Flower {
        return dataRepo.findFlower()
    }

}


