package com.foo.rest.examples.spring.resource;

import com.foo.rest.examples.spring.db.SpringWithDbController;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceRestController extends SpringWithDbController {

    public ResourceRestController() {
        super(ResourceApplication.class);
    }
}
