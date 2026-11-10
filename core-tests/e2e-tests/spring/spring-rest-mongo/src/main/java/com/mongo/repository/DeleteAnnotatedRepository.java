package com.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface DeleteAnnotatedRepository extends MongoRepository<DeleteAnnotatedData, String> {

    @Query(value = "{ 'city': ?0, 'age': { $gte: ?1 } }", delete = true)
    long deleteAnnotated(String city, int minAge);
}
