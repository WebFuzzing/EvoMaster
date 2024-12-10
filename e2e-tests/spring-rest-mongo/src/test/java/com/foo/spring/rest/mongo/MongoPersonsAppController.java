package com.foo.spring.rest.mongo;

import com.mongo.persons.MongoPersonsApp;

public class MongoPersonsAppController extends MongoController{
    public MongoPersonsAppController() {
        super("persons", MongoPersonsApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.persons";
    }
}
