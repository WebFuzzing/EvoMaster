package com.foo.spring.rest.mongo.template;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.template.MongoTemplateApp;

public class MongoTemplateAppController extends MongoController {
    public MongoTemplateAppController() {
        super("template", MongoTemplateApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.template";
    }
}
