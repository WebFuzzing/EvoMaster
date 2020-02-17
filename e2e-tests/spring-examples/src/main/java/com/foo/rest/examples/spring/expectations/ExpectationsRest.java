package com.foo.rest.examples.spring.expectations;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/expectations")
public class ExpectationsRest {
    @RequestMapping(
            value = "/getExpectations/{b}",
            method = RequestMethod.GET
    )
    public String expectTest(
            @PathVariable("b") boolean succeeded
    ) throws IllegalArgumentException{
        if (succeeded) return "Success is True!";
        else throw new IllegalArgumentException("Input Was False");
    }

}
