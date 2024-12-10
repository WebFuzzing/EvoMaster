package com.foo.graphql.alllimitreached.resolver

import com.foo.graphql.alllimitreached.DataRepository
import com.foo.graphql.alllimitreached.type.Book

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
