package com.foo.rest.examples.spring.db.disablesql;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import com.foo.rest.examples.spring.db.javatypes.JavaTypesApp;

public class DisableSqlController extends SpringWithDbController {

    public DisableSqlController() {
        super(DisableSqlApp.class);
    }

}
