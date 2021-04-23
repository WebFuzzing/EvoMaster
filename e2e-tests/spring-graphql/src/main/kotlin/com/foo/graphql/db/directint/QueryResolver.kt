package com.foo.graphql.db.directint

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class QueryResolver(private val repository : DbDirectIntRepository) : GraphQLQueryResolver {

    fun get(x: Int, y : Int) : List<DbDirectInt>{
        val found = repository.findByXAndY(x, y)
        //just for adding a target related to data
        if (found.isNotEmpty())
            return found
        return listOf()
    }
}

