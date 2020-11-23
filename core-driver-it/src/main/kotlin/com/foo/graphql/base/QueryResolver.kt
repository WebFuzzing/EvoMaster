package com.foo.graphql.base

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val repository: UserRepository
)
    : GraphQLQueryResolver {


    fun all(): List<UserType> = repository.allUsers().toList()
}