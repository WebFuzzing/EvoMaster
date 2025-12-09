package com.foo.rest.examples.spring.chainedheaderlocation;

import com.foo.rest.examples.spring.SpringController;

public class CHLController extends SpringController {

    public CHLController() {
        super(CHLApplication.class);
    }

    @Override
    public void resetStateOfSUT() {
        CHLRest.data.clear();
    }
}
