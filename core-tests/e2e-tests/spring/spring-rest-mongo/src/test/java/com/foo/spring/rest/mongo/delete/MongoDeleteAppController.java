package com.foo.spring.rest.mongo.delete;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.delete.MongoDeleteApp;

public class MongoDeleteAppController extends MongoController {
    public MongoDeleteAppController() {
        super("delete", MongoDeleteApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.delete";
    }
}
