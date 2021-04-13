package com.foo.graphql.db.base

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class QueryResolver(private val repository : DbBaseRepository) : GraphQLQueryResolver {

    fun all() : List<DbBase>{
        return repository.findAll().toList()
    }

    fun dbBaseByName(name : String) : List<DbBase>{
        return repository.findByName(name).toList()
    }
}

