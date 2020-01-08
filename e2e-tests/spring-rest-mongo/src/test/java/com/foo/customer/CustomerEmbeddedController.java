package com.foo.customer;

import com.foo.spring.rest.mongo.SpringRestMongoController;

public class CustomerEmbeddedController extends SpringRestMongoController {

    public CustomerEmbeddedController() {
        super(CustomerApplication.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.customer";
    }
}
