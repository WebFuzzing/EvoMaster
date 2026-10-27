package com.foo.spring.rest.mongo.findoneandupdate;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.findoneandupdate.MongoFindOneAndUpdateApp;

public class MongoFindOneAndUpdateAppController extends MongoController {
    public MongoFindOneAndUpdateAppController() {
        super("findoneandupdate", MongoFindOneAndUpdateApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.findoneandupdate";
    }
}
