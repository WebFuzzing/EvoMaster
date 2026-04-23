package com.foo.spring.rest.mongo;

import com.mongo.students.MongoStudentsApp;

public class MongoStudentsAppController extends MongoController {
    public MongoStudentsAppController() {
        super("students", MongoStudentsApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.students";
    }

}
