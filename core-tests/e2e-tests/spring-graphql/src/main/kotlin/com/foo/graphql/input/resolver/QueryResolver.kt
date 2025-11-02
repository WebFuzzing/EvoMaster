package com.foo.graphql.input.resolver


import com.foo.graphql.input.DataRepository
import com.foo.graphql.input.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {



    fun flowersById(id: Int): Flower?{
        return dataRepo.findById(id)
    }

}


