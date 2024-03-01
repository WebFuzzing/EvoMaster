package com.foo.graphql.mutationObject.resolver

import com.foo.graphql.mutationObject.DataRepository

import com.foo.graphql.mutationObject.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val repository: DataRepository
)
    : GraphQLQueryResolver {


    fun flowers(): List<Flower?>? = repository.allFlowers()?.toList()


}


