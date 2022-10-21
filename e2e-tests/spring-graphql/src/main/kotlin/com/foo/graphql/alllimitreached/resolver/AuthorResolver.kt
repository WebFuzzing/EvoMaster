package com.foo.graphql.alllimitreached.resolver

import com.foo.graphql.alllimitreached.DataRepository
import com.foo.graphql.alllimitreached.type.Address
import com.foo.graphql.alllimitreached.type.Author

import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class AuthorResolver (private val dataRepo: DataRepository): GraphQLResolver<Author> {

    fun getAdress(author: Author): Address {
        return dataRepo.findAddressAuthBy(author)

    }
}