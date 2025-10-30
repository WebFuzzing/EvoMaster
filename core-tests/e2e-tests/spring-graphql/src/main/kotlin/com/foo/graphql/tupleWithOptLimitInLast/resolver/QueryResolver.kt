package com.foo.graphql.tupleWithOptLimitInLast.resolver


import com.foo.graphql.tupleWithOptLimitInLast.DataRepository
import com.foo.graphql.tupleWithOptLimitInLast.type.Flower
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


