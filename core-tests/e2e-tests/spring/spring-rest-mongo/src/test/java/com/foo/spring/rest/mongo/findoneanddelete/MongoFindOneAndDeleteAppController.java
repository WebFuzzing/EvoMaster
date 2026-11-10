package com.foo.spring.rest.mongo.findoneanddelete;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.findoneanddelete.MongoFindOneAndDeleteApp;

public class MongoFindOneAndDeleteAppController extends MongoController {
    public MongoFindOneAndDeleteAppController() {
        super("findoneanddelete", MongoFindOneAndDeleteApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.findoneanddelete";
    }
}
