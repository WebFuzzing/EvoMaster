package com.foo.spring.rest.polyglotpersistence

import org.springframework.data.mongodb.repository.MongoRepository

interface MongoPersonRepository : MongoRepository<MongoPerson, String> {
    fun findByName(name: String): List<MongoPerson>
}
