package com.foo.graphql.db.directint

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class QueryResolver(private val repository : DbDirectIntRepository) : GraphQLQueryResolver {

    fun get(x: Int, y : Int) : List<DbDirectInt>{
        return repository.findByXAndY(x, y)
    }
}

