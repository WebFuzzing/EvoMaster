package com.foo.rest.examples.spring.taintMulti;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.taint.TaintApplication;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintMultiController extends SpringController {

    public TaintMultiController(){
        super(TaintMultiApplication.class);
    }
}
