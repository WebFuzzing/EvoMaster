package com.foo.spring.rest.mongo.findstring;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.findstring.MongoFindStringApp;

public class MongoFindStringController extends MongoController {
    public MongoFindStringController() {
        super("findstring", MongoFindStringApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.findstring";
    }

}
