package com.foo.graphql.alllimitreached


import com.foo.graphql.alllimitreached.type.Address
import com.foo.graphql.alllimitreached.type.Author
import com.foo.graphql.alllimitreached.type.Book
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger


@Component
open class DataRepository {

    private val counter = AtomicInteger(0)

        private val authors = listOf(
            Author( Address("address-1","street name-1")),
            Author( Address("address-2","street name-2")),
            Author( Address("address-3","street name-3"))
        )

        private val books =listOf( Book( Author( Address("address-1","street name-1")),),
            Book(  Author(Address("address-2","street name-2")),
            ),
            Book(Author(Address("address-3","street name-3"))
            )
        )

    fun books(): Collection<Book> {
        return books
    }

    fun findAuthorById(book: Book): Author? {
        return authors.get(0)
    }


    fun findAddressAuthBy(author: Author): Address {

        return authors.get(0).address
    }

    fun saveBook( name:String?): String{
        val id = counter.getAndIncrement()
        return Book(Author( Address("address-X","street name-X"))).toString()

    }
}




