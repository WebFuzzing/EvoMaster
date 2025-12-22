package com.foo.rest.examples.spring.ttpaper;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
public class TTPaperParam {


    @GetMapping("/api/param")
    public String get(WebRequest wr){

        String param = wr.getParameter("param");

        if(param.equals("FOO")){
            return "OK";
        }

        return null;
    }
}
