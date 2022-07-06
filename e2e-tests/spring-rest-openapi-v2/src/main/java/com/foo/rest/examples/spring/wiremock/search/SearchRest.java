package com.foo.rest.examples.spring.wiremock.search;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/wiremock")
public class SearchRest {

    @RequestMapping(
            value = "/search/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public Boolean equalsFoo(@PathVariable("s") String s) {
        return "foo".equals(s);
    }

    @RequestMapping(
            value = "/search/return/foo",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String returnFoo() {
        return "foo";
    }
}
