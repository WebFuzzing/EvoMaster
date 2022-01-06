package com.foo.micronaut.rest;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Controller("/calculate")
public class CalculateController {

    @Get(value = "{?x,y}", produces = MediaType.TEXT_PLAIN)
    public String index(@NotNull @PositiveOrZero Integer x, @NotNull @PositiveOrZero Integer y) {
        Integer z = x + y;
        return z.toString();
    }
}
