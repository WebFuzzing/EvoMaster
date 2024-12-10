package com.foo.spring.rest.mongo.findoneby;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.findoneby.MongoFindOneByApp;

public class MongoFindOneByController extends MongoController {
    public MongoFindOneByController() {
        super("findoneby", MongoFindOneByApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.findoneby";
    }

}
