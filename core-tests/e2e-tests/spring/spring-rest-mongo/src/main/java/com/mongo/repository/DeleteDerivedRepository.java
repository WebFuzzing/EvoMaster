package com.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeleteDerivedRepository extends MongoRepository<DeleteDerivedData, String> {

    long deleteByCityAndAgeGreaterThanEqual(String city, int minAge);
}
