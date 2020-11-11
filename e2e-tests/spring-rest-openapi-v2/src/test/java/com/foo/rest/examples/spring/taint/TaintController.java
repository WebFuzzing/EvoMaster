package com.foo.rest.examples.spring.taint;

import com.foo.rest.examples.spring.SpringController;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintController extends SpringController {

    public TaintController(){
        super(TaintApplication.class);
    }
}
