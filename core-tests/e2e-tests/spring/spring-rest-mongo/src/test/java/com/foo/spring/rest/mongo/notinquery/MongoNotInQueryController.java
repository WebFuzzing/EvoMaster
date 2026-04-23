package com.foo.spring.rest.mongo.notinquery;

import com.foo.spring.rest.mongo.MongoController;
import com.mongo.findstring.MongoFindStringApp;
import com.mongo.notinquery.MongoNotInQueryApp;

public class MongoNotInQueryController extends MongoController {
    public MongoNotInQueryController() {
        super("notinquery", MongoNotInQueryApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.notinquery";
    }

}
