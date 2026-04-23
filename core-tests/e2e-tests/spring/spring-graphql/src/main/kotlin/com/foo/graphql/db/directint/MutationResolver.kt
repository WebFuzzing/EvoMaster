package com.foo.graphql.db.directint

import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class MutationResolver(private val repository : DbDirectIntRepository) : GraphQLMutationResolver {

    fun addDbDirectInt() : DbDirectInt {
        return repository.save(DbDirectInt(x= 42, y = 77))
    }
}