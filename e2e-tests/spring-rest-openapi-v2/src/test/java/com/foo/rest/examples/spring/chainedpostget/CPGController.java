package com.foo.rest.examples.spring.chainedpostget;

import com.foo.rest.examples.spring.SpringController;

public class CPGController extends SpringController {

    public CPGController() {
        super(CPGApplication.class);
    }

    @Override
    public void resetStateOfSUT() {
        CPGRest.data.clear();
    }
}
