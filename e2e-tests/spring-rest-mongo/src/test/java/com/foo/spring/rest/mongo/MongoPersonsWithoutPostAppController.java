package com.foo.spring.rest.mongo;

import com.mongo.personswithoutpost.MongoPersonsWithoutPostApp;

public class MongoPersonsWithoutPostAppController extends MongoController{
    public MongoPersonsWithoutPostAppController() {
        super("persons", MongoPersonsWithoutPostApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.personswithoutpost";
    }
}
