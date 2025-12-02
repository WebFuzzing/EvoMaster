package com.foo.rest.examples.spring.tcpport;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.taintMulti.TaintMultiApplication;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TcpPortController extends SpringController {

    public TcpPortController(){
        super(TcpPortApplication.class);
    }
}
