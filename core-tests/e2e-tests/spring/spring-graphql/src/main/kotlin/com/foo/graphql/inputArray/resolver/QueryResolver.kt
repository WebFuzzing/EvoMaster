package com.foo.graphql.inputArray.resolver


import com.foo.graphql.inputArray.DataRepository
import com.foo.graphql.inputArray.type.Flower
import com.foo.graphql.inputArray.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowers(store: Array<Store>): Flower?{
        return dataRepo.findFlower(store)
    }

}


