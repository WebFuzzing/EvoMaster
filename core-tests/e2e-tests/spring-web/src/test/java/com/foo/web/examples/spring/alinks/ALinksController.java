package com.foo.web.examples.spring.alinks;

import com.foo.web.examples.spring.SpringController;
import com.foo.web.examples.spring.base.BaseWebApplication;

public class ALinksController extends SpringController {

    public ALinksController() {
        super(BaseWebApplication.class, "/alinks/index.html");
    }
}
