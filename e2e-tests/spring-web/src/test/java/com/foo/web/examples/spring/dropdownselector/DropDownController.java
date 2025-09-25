package com.foo.web.examples.spring.dropdownselector;

import com.foo.web.examples.spring.SpringController;
import com.foo.web.examples.spring.base.BaseWebApplication;

public class DropDownController extends SpringController {
    public DropDownController() {
        super(BaseWebApplication.class, "/dropdown/index.html");
    }

}
