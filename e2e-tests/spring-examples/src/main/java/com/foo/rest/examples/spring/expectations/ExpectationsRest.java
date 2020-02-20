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

    @RequestMapping(
            value = "/basicResponsesString/{b}",
            method = RequestMethod.GET
    )
    public String basicResponsesString(
            @PathVariable("b") boolean succeeded
    ) {
        if (succeeded) return "Success is True!";
        else return "What do you mean no string?";
    }

    @RequestMapping(
            value = "/basicNumbersString/{b}",
            method = RequestMethod.GET
    )
    public int basicResponsesNumber(
            @PathVariable("b") boolean succeeded
    ) {
        if (succeeded) return 42;
        else return -1;
    }
}
