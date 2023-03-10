package com.foo.web.examples.spring.noaction;

import com.foo.web.examples.spring.SpringController;
import com.foo.web.examples.spring.base.BaseWebApplication;

public class NoActionController extends SpringController {

    public NoActionController() {
        super(BaseWebApplication.class, "/noaction/index.html");
    }
}
