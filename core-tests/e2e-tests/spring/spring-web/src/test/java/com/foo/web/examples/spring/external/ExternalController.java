package com.foo.web.examples.spring.external;

import com.foo.web.examples.spring.SpringController;
import com.foo.web.examples.spring.base.BaseWebApplication;

public class ExternalController extends SpringController {

    public ExternalController() {
        super(BaseWebApplication.class, "/external/index.html");
    }
}
