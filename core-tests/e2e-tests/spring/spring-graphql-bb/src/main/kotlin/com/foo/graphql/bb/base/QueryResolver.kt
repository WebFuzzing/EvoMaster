package com.foo.graphql.bb.base

import graphql.kickstart.tools.GraphQLQueryResolver
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val repository: UserRepository
)
    : GraphQLQueryResolver {


    fun all(): List<UserType> {
        CoveredTargets.cover("ALL")
        return repository.allUsers().toList()
    }
}
