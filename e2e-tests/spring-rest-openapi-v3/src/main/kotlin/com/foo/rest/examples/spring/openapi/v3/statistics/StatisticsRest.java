package com.foo.rest.examples.spring.openapi.v3.statistics;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/statistics")
public class StatisticsRest {
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
