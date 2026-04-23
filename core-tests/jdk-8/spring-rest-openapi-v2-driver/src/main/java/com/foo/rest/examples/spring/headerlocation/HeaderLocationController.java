package com.foo.rest.examples.spring.headerlocation;

import com.foo.rest.examples.spring.SpringController;

public class HeaderLocationController extends SpringController {

    public HeaderLocationController() {
        super(HeaderLocationApplication.class);
    }

    @Override
    public void resetStateOfSUT() {
        HeaderLocationRest.data.clear();
    }
}
