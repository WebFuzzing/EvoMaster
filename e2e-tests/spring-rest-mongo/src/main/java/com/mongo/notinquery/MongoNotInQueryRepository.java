package com.mongo.notinquery;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoNotInQueryRepository extends MongoRepository<MongoNotInQueryData, String> {
}
