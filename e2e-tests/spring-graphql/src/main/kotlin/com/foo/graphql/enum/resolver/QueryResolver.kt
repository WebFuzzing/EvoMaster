package com.foo.graphql.enum.resolver


import com.foo.graphql.enum.DataRepository
import com.foo.graphql.enum.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver{

        fun flowers(): List<Flower>{
            return  dataRepo.allFlowers().toList()
        }


}
