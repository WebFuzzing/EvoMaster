package com.foo.micronaut.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller()
public class IndexController {

    @Operation(summary = "Index controller to crash micronaut with 500",
            description = "To test the crash scenario."
    )
    @ApiResponse(responseCode = "500", description = "Expected outcome")
    @Get(produces = MediaType.TEXT_PLAIN)
    public HttpResponse<String> index() {
        // it is expected the application to send connection close when it crashes
//        throw new ExperimentalException();
        return HttpResponse.status(HttpStatus.OK).body("OK");
    }

    @Operation(summary = "POST Controller for test",
            description = "Return 200"
    )
    @ApiResponse(responseCode = "200", description = "Working POST route")
    @Post(produces = MediaType.TEXT_PLAIN)
    public String indexPost() {
        return "Viola!";
    }

}
