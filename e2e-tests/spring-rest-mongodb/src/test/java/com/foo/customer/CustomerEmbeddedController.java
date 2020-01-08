package com.foo.customer;

import com.foo.spring.rest.mongodb.SpringRestMongodbController;

public class CustomerEmbeddedController extends SpringRestMongodbController {

    public CustomerEmbeddedController() {
        super(CustomerApplication.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.customer";
    }
}
