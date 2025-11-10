package com.foo.graphql.bb.base

import org.springframework.stereotype.Component

@Component
open class UserRepository {

    private val users = mutableMapOf<String, UserType>()


    init {
        listOf(UserType("0", "Foo", "Bar", 42),
                UserType("1", "Joe", "Black", 18),
                UserType("2", "John", "Smith", 7),
                UserType("3", "Mario", "Rossi", 25)
        ).forEach { users[it.id] = it }

    }

    fun allUsers(): Collection<UserType> = users.values

    fun findById(id: String) = users[id]
}