package com.foo.rest.examples.spring.ttpaper;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TTPaperNumeric {

    @GetMapping(path = "/api/numeric/{x}")
    public String get(@PathVariable("x") double x){

        double y = x * 2;
        boolean a = (y > 42.01 && y < 42.02);

        if(a) return "OK";
        else return null;
    }
}
