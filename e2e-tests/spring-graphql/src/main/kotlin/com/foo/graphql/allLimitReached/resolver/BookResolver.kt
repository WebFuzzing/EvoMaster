package com.foo.graphql.allLimitReached.resolver

import com.foo.graphql.allLimitReached.DataRepository
import com.foo.graphql.allLimitReached.type.Address
import com.foo.graphql.allLimitReached.type.Author
import com.foo.graphql.allLimitReached.type.Book

import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class BookResolver (private val dataRepo: DataRepository): GraphQLResolver<Book> {

    //fun getId(book: Book) = book.id
    //fun getName(book: Book) = book.name

    /*fun getAuthor(book: Book): Author {
        return dataRepo.findAuthorById(book.author.id)
                ?: Author("author-X", "author-X-last-name", "author-X-first-name", Address("address-X","street name-X"))
    }*/

    fun getAuthor(book: Book): Author {
        return dataRepo.findAuthorById(book)
            ?: Author(Address("address-X", "street name-X"))


    }
}