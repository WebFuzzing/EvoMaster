package com.foo.graphql.functionInReturnedObject.resolver


import com.foo.graphql.functionInReturnedObject.DataRepository
import com.foo.graphql.functionInReturnedObject.type.Flower
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


