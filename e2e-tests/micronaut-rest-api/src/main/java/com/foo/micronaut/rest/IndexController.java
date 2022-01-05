package com.foo.micronaut.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/index")
public class IndexController {

    @Operation(summary = "Index controller to crash micronaut with 500",
            description = "To test the crash scenario."
    )
    @ApiResponse(responseCode = "500", description = "Expected outcome")
    @Get(produces = MediaType.TEXT_PLAIN)
    public HttpResponse<String> index() {
        throw new ExperimentalException();   // it is expected the application to send connection close when it crashes
//        return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Crashed successfully!");
    }

    @Operation(summary = "POST Controller for test",
            description = "Return 200"
    )
    @ApiResponse(responseCode = "200", description = "Working POST route")
    @Post(produces = MediaType.TEXT_PLAIN)
    public String indexPost() {
        return "Viola!";
    }


    @Put(value = "/noroute", produces = MediaType.TEXT_PLAIN)
    public HttpResponse<String> noroute() {
        return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED).body("Not implemented");
    }

    @Patch(value = "/outoforder", produces = MediaType.TEXT_PLAIN)
    public HttpResponse<String> outoforder() {
        return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Out of order!");
    }
}
