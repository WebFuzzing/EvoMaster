package com.foo.graphql.alllimitreached.resolver

import com.foo.graphql.alllimitreached.DataRepository
import com.foo.graphql.alllimitreached.type.Address
import com.foo.graphql.alllimitreached.type.Author
import com.foo.graphql.alllimitreached.type.Book

import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class BookResolver (private val dataRepo: DataRepository): GraphQLResolver<Book> {

    fun getAuthor(book: Book): Author {
        return dataRepo.findAuthorById(book)
            ?: Author(Address("address-X", "street name-X"))


    }
}