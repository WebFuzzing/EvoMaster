package com.foo.spring.rest.mongo;

import com.mongo.objectid.MongoObjectIdApp;

public class MongoObjectIdAppController extends MongoController {
    public MongoObjectIdAppController() {
        super("objectid", MongoObjectIdApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.objectid";
    }

}
