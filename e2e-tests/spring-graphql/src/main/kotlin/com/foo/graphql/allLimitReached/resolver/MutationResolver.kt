package com.foo.graphql.allLimitReached.resolver

import com.foo.graphql.allLimitReached.DataRepository
import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class MutationResolver(
        private val dataRepo: DataRepository
) : GraphQLMutationResolver {

    fun addBook( name: String?): String {
        return dataRepo.saveBook(name)

    }

}




