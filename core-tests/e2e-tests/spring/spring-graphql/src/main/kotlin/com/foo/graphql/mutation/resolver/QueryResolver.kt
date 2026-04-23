package com.foo.graphql.mutation.resolver

import com.foo.graphql.mutation.DataRepository

import com.foo.graphql.mutation.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val repository: DataRepository
)
    : GraphQLQueryResolver {


    fun flowers(): List<Flower> = repository.allFlowers().toList()


}


