package com.foo.graphql.db.base

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class QueryResolver(private val repository : DbBaseRepository) : GraphQLQueryResolver {

    fun all() : List<DbBase>{
        return repository.findAll().toList()
    }

    fun dbBaseByName(name : String) : List<DbBase>{
        val results = repository.findByName(name).toList()
        if (results.isNotEmpty()){
            //add an object only for test
            return results.plus(DbBase(42, "foo"))
        }
        return listOf()
    }
}

