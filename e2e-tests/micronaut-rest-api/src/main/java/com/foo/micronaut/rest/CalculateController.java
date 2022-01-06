package com.foo.micronaut.rest;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import javax.annotation.Nullable;
import javax.validation.constraints.PositiveOrZero;

@Controller("/calculate")
public class CalculateController {

    @Get(value = "{?x,y}", produces = MediaType.TEXT_PLAIN)
    public int index(@Nullable @PositiveOrZero Integer x, @Nullable @PositiveOrZero Integer y) {
        return ( x != null && y != null) ? x + y : 0;
    }
}
