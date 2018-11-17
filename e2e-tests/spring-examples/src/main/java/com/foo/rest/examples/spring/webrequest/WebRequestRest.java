package com.foo.rest.examples.spring.webrequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
public class WebRequestRest {


    @GetMapping("/api/webrequest")
    public String get(WebRequest wr){

        String a = wr.getParameter("a");
        String b = wr.getParameter("b");

        if(a == null || b == null){
            return "FALSE";
        }

        return "TRUE";
    }
}
