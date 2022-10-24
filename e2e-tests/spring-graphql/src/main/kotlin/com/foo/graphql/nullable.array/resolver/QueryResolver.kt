package com.foo.graphql.nullable.array.resolver


import com.foo.graphql.nullable.array.DataRepository
import com.foo.graphql.nullable.array.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowers(id: Array<Int?>?): Flower?{
        return dataRepo.findFlowers(id)
    }

}


