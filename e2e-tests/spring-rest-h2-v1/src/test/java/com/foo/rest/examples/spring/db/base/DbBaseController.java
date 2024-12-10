package com.foo.rest.examples.spring.db.base;

import com.foo.rest.examples.spring.db.SpringWithDbController;

public class DbBaseController extends SpringWithDbController {

    public DbBaseController(){
        super(DbBaseApplication.class);
    }
}
