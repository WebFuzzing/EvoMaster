package com.foo.graphql.functionInReturnedObjectWithReturnPrimitives.resolver


import com.foo.graphql.functionInReturnedObjectWithReturnPrimitives.DataRepository
import com.foo.graphql.functionInReturnedObjectWithReturnPrimitives.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {

    fun flowers(): Flower{
        return dataRepo.findFlower()
    }

}


