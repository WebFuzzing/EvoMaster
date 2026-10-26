package com.foo.spring.rest.mongo.findoneandreplace;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.findoneandreplace.MongoFindOneAndReplaceApp;

public class MongoFindOneAndReplaceAppController extends MongoController {
    public MongoFindOneAndReplaceAppController() {
        super("findoneandreplace", MongoFindOneAndReplaceApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.findoneandreplace";
    }
}
