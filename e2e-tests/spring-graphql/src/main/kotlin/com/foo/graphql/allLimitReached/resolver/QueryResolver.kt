package com.foo.graphql.allLimitReached.resolver

import com.foo.graphql.allLimitReached.DataRepository
import com.foo.graphql.allLimitReached.type.Book

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component


@Component
open class QueryResolver(
        private val repository: DataRepository
)
    : GraphQLQueryResolver {


    fun books(): List<Book>{
        return  repository.books().toList()
    }

}
