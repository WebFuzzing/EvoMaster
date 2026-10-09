package com.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CountDerivedRepository extends MongoRepository<CountDerivedData, String> {

    long countByCityAndAgeGreaterThanEqual(String city, int minAge);
}
