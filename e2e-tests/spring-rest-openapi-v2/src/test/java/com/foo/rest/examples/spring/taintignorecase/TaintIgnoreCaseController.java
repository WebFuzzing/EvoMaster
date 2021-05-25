package com.foo.rest.examples.spring.taintignorecase;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.taint.TaintApplication;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintIgnoreCaseController extends SpringController {

    public TaintIgnoreCaseController(){
        super(TaintIgnoreCaseApplication.class);
    }
}
