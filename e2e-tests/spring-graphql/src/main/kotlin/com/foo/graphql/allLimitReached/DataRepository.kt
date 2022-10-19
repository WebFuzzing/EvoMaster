package com.foo.graphql.allLimitReached


import com.foo.graphql.allLimitReached.type.Address
import com.foo.graphql.allLimitReached.type.Author
import com.foo.graphql.allLimitReached.type.Book
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger


@Component
open class DataRepository {


    /*private val authors = mutableMapOf<String, Author>()
    private val books = mutableMapOf<String, Book>()
    private val counter = AtomicInteger(0)
    init {
        listOf(
        Author("author-1", "author-1-first-name", "author-1-last-name", Address("address-1","street name-1")),
        Author("author-2", "author-2-first-name", "author-2-last-name", Address("address-2","street name-2")),
        Author("author-3", "author-3-first-name", "author-3-last-name", Address("address-3","street name-3"))
        ).forEach { authors[it.id] = it }

        listOf( Book("book-1", "book-1-name",  Author("author-1", "author-1-first-name", "author-1-last-name", Address("address-1","street name-1")),),
            Book("book-2", "book-2-name",  Author("author-2", "author-2-first-name", "author-2-last-name", Address("address-2","street name-2")),
            ),
            Book("book-3", "book-3-name",  Author("author-3", "author-3-first-name", "author-3-last-name", Address("address-3","street name-3"))
            )

        ).forEach { books[it.id] = it }

    }*/

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




    /*fun books(): Collection<Book> {//In the query
        return books.values
    }*/

    fun books(): Collection<Book> {//In the query
        return books
    }

   /* fun findAuthorById(id: String): Author? {
        return authors[id]
    }*/

    fun findAuthorById(book: Book): Author? {
        return authors.get(0)
    }

  /*  fun findAddressAuthBy(id: String): Address {

        return authors[id]?.address ?: Address("address-X", "street name-X")
    }*/

    fun findAddressAuthBy(author: Author): Address {//second

        return authors.get(0).address ?: Address("address-X", "street name-X")
    }

  /*  fun saveBook( name:String): Book{
        val id = counter.getAndIncrement()
        return Book(id.toString(), name, Author("author-X", "author-X-last-name", "author-X-first-name", Address("address-X","street name-X")))

    }*/
   /* fun saveBook( name:String): Book{
        val id = counter.getAndIncrement()
        return Book(Author( Address("address-X","street name-X")))

    }*/

    fun saveBook( name:String?): String{
        val id = counter.getAndIncrement()
        return Book(Author( Address("address-X","street name-X"))).toString()

    }
}




