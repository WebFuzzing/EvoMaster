package com.foo.graphql.primitiveType

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowersById(id: Int?): String? {
        return dataRepo.findById(id)
    }


}


