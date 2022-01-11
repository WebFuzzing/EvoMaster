package com.foo.micronaut.patio;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller
public class IndexController {

    @Operation(summary = "Index controller to crash micronaut with 500",
            description = "To test the crash scenario."
    )
    @ApiResponse(responseCode = "500", description = "Expected outcome")
    @Get(produces = MediaType.APPLICATION_JSON)
    public HttpResponse index() {
        return HttpResponse.serverError().body("{\"message\":\"Crashed\"}");
    }

}
