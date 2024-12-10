package com.foo.graphql.optionalPrimitiveArrayInput.resolver


import com.foo.graphql.optionalPrimitiveArrayInput.DataRepository
import com.foo.graphql.optionalPrimitiveArrayInput.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {



    fun flowersById(id: Array<Int?>?): Flower?{//opt array in and out following the GQL schema
        return dataRepo.findById(id)
    }

}


