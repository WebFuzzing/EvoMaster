package com.foo.graphql.enumIntupleArg.resolver


import com.foo.graphql.enumIntupleArg.DataRepository
import com.foo.graphql.enumIntupleArg.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {

    fun flowers(): Flower? {
        return dataRepo.findFlower()
    }

}


