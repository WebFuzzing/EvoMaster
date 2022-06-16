package com.foo.rest.examples.spring.endpoints;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.constant.ConstantApplication;

public class EndpointsController extends SpringController {

    public EndpointsController(){
        super(EndpointsApplication.class);
    }
}
