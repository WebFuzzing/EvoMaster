package com.foo.rest.examples.spring.namedresource;

import com.foo.rest.examples.spring.SpringController;

/**
 * Created by arcand on 01.03.17.
 */
public class NamedResourceController extends SpringController {

    public NamedResourceController() {
        super(NamedResourceApplication.class);
    }

    @Override
    public void resetStateOfSUT() {
        NamedResourceRest.data.clear();
    }
}
