package com.foo.spring.rest.multidb.mongoredispostgres

import org.springframework.data.mongodb.repository.MongoRepository

interface MongoPersonRepository : MongoRepository<MongoPerson, String> {
    fun findByAge(age: Int): List<MongoPerson>
}
