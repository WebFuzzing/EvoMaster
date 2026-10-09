package com.foo.spring.rest.mongo.update;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.update.MongoUpdateApp;

public class MongoUpdateAppController extends MongoController {
    public MongoUpdateAppController() {
        super("update", MongoUpdateApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.update";
    }
}
