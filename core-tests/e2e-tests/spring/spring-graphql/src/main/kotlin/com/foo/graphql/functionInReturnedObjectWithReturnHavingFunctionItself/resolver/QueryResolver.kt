package com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself.resolver


import com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself.DataRepository
import com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself.type.Flower
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


