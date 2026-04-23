package com.foo.graphql.db.base

import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class MutationResolver(private val repository : DbBaseRepository) : GraphQLMutationResolver {

    fun addDbBase(name : String?) : DbBase {
        return repository.save(DbBase(name= name))
    }
}