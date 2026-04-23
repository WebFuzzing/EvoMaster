package com.foo.web.examples.spring.wronglinks;

import com.foo.web.examples.spring.SpringController;
import com.foo.web.examples.spring.base.BaseWebApplication;

public class WrongLinksController extends SpringController {

    public WrongLinksController() {
        super(BaseWebApplication.class, "/wronglinks/index.html");
    }
}
