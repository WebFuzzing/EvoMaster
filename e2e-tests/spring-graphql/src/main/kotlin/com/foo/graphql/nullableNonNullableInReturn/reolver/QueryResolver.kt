package com.foo.graphql.nullableNonNullableInReturn.reolver

import com.foo.graphql.nullableNonNullableInReturn.DataRepository
import com.foo.graphql.nullableNonNullableInReturn.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component


@Component

open class QueryResolver(
        private val repository: DataRepository
)
    : GraphQLQueryResolver {


    fun flower(): Flower? = repository.flowerN()

}