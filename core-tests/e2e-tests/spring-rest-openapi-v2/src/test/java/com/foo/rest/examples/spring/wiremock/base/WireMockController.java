package com.foo.rest.examples.spring.wiremock.base;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.wiremock.base.WireMockApplication;

public class WireMockController extends SpringController {

    public WireMockController() {
        super(WireMockApplication.class);
    }
}
