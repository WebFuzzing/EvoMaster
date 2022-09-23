package com.foo.graphql.allLimitReached.resolver

import com.foo.graphql.allLimitReached.DataRepository
import com.foo.graphql.allLimitReached.type.Address
import com.foo.graphql.allLimitReached.type.Author

import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class AuthorResolver (private val dataRepo: DataRepository): GraphQLResolver<Author> {

    //fun getId(author: Author) = author.id
    //fun getName(author: Author) = author.firstName
    //fun getlastName(author: Author) = author.lastName
    /*fun getAdress(author: Author): Address {
        return dataRepo.findAddressAuthBy(author.id)

    }*/

    fun getAdress(author: Author): Address {
        return dataRepo.findAddressAuthBy(author)

    }
}