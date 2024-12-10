package com.foo.web.examples.spring.malformedhtml;

import com.foo.web.examples.spring.SpringController;
import com.foo.web.examples.spring.base.BaseWebApplication;

public class MalformedHtmlController extends SpringController {

    public MalformedHtmlController() {
        super(BaseWebApplication.class, "/malformedhtml/index.html");
    }
}
