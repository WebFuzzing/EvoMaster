package com.foo.graphql.allLimitReached.resolver

import com.foo.graphql.allLimitReached.DataRepository
import com.foo.graphql.allLimitReached.type.Address
import com.foo.graphql.allLimitReached.type.Author
import com.foo.graphql.allLimitReached.type.Book

import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class BookResolver (private val dataRepo: DataRepository): GraphQLResolver<Book> {

    fun getAuthor(book: Book): Author {
        return dataRepo.findAuthorById(book)
            ?: Author(Address("address-X", "street name-X"))


    }
}