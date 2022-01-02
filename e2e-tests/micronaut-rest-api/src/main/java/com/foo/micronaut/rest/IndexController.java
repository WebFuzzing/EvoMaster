package com.foo.micronaut.rest;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/status")
public class IndexController {

    @Get(produces = MediaType.TEXT_PLAIN)
    public String index() {
        return "Viola!";
    }
}
