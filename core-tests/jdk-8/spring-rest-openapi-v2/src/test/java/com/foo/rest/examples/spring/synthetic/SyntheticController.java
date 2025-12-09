package com.foo.rest.examples.spring.synthetic;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.strings.StringsApplication;

public class SyntheticController extends SpringController {

    public SyntheticController(){
        super(SyntheticApplication.class);
    }
}
