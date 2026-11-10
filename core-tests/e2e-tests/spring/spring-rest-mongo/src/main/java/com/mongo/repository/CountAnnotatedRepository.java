package com.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CountAnnotatedRepository extends MongoRepository<CountAnnotatedData, String> {

    @Query(value = "{ 'city': ?0, 'age': { $gte: ?1 } }", count = true)
    long countAnnotated(String city, int minAge);
}
