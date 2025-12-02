package com.foo.rest.examples.spring.scheduled;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.strings.StringsApplication;

public class ScheduledController extends SpringController {

    public ScheduledController(){
        super(ScheduledApplication.class);
    }
}
