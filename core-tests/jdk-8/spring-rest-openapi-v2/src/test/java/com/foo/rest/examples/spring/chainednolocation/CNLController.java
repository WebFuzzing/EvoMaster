package com.foo.rest.examples.spring.chainednolocation;

import com.foo.rest.examples.spring.SpringController;

public class CNLController extends SpringController {

    public CNLController() {
        super(CNLApplication.class);
    }

    @Override
    public void resetStateOfSUT() {
        CNLRest.data.clear();
    }
}