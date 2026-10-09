package com.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExistsDerivedRepository extends MongoRepository<ExistsDerivedData, String> {

    boolean existsByCityAndAgeGreaterThanEqual(String city, int minAge);
}
