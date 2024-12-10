package com.mongo.findstring;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoFindStringRepository  extends MongoRepository<MongoFindStringData, String> {
}
