package com.foo.graphql.inputObject.resolver


import com.foo.graphql.inputObject.DataRepository
import com.foo.graphql.inputObject.type.Flower
import com.foo.graphql.inputObject.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowers(store: Store): Flower?{
        return dataRepo.findFlower(store)
    }

}


