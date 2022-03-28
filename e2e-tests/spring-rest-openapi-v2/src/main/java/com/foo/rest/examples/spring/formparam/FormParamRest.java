package com.foo.rest.examples.spring.formparam;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class FormParamRest {

    @PostMapping(value = "/api/formparam", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String post(@RequestParam("x") Integer x, @RequestParam("y") Integer y){

        if(x < 0 && y > 0){
            return "OK";
        }

        return null;
    }
}
