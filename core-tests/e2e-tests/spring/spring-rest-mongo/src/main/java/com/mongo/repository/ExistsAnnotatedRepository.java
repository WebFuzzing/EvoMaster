package com.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ExistsAnnotatedRepository extends MongoRepository<ExistsAnnotatedData, String> {

    @Query(value = "{ 'city': ?0, 'age': { $gte: ?1 } }", exists = true)
    boolean existsAnnotated(String city, int minAge);
}
