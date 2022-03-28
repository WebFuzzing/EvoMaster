package com.foo.rest.examples.spring.redirect;

import com.foo.rest.examples.spring.SpringController;

/**
 * Created by arcuri82 on 10-Sep-19.
 */
public class RedirectController extends SpringController {

    /*
        Here we use default swagger from Springfox, which currently
        does not support pattern regex.
        However, with taint analysis we should still be able to handle it.
     */

    public RedirectController() {
        super(RedirectApplication.class);
    }
}
