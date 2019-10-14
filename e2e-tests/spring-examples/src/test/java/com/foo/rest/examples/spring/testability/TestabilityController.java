package com.foo.rest.examples.spring.testability;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.taintMulti.TaintMultiApplication;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TestabilityController extends SpringController {

    public TestabilityController(){
        super(TestabilityApplication.class);
    }
}
