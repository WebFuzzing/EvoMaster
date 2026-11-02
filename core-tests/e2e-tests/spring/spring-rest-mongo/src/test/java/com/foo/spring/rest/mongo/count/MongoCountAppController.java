package com.foo.spring.rest.mongo.count;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.count.MongoCountApp;

public class MongoCountAppController extends MongoController {
    public MongoCountAppController() {
        super("count", MongoCountApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.count";
    }
}
