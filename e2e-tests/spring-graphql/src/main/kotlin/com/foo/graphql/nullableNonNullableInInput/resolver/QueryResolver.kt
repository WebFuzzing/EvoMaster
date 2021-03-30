package com.foo.graphql.nullableNonNullableInInput.resolver


import com.foo.graphql.nullableNonNullableInInput.DataRepository
import com.foo.graphql.nullableNonNullableInInput.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {



    fun flowersByIdNnN(id: Int): Flower?{
        return dataRepo.findById(id)
    }

    fun flowersByIdN(id: Int): Flower?{
        return dataRepo.findById(id)
    }

}


