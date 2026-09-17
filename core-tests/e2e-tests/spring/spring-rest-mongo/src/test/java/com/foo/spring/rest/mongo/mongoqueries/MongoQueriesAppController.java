package com.foo.spring.rest.mongo.mongoqueries;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.mongoqueries.MongoQueriesApp;

public class MongoQueriesAppController extends MongoController {

    public MongoQueriesAppController() {
        super("mongoqueries", MongoQueriesApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.mongoqueries";
    }
}
