package com.foo.rest.examples.spring.bodyissue;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by arcuri82 on 07-Nov-18.
 */
@RestController
public class BodyIssueRest {


    @PostMapping(value = "/api/bodyissue", consumes = "application/x-www-form-urlencoded")
    public int postUrl( ){
        return 42;
    }

    @PostMapping(value = "/api/bodyissue", consumes = "application/json")
    public int postJson( @RequestBody(required = true) Object dto){
        return 123;
    }


}
