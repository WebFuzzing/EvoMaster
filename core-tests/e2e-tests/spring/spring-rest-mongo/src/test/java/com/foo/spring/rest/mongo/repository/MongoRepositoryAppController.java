package com.foo.spring.rest.mongo.repository;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.repository.MongoRepositoryApp;

public class MongoRepositoryAppController extends MongoController {
    public MongoRepositoryAppController() {
        super("repository", MongoRepositoryApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.repository";
    }
}
