package com.foo.rest.examples.spring.escape;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.escapes.EscapeApplication;
import com.foo.rest.examples.spring.strings.StringsApplication;

public class EscapeController extends SpringController {

    public EscapeController(){
        super(EscapeApplication.class);
    }
}
