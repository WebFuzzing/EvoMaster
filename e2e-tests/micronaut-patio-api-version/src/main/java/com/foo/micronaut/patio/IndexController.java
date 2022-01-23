package com.foo.micronaut.patio;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.HashSet;
import java.util.Set;

@Controller
public class IndexController {

    private static final Set<Integer> ports = new HashSet<>();

    @Operation(summary = "Index controller to crash micronaut with 500",
            description = "To test the crash scenario."
    )
    @ApiResponse(responseCode = "500", description = "Expected outcome")
    @Get(produces = MediaType.APPLICATION_JSON)
    public HttpResponse index() {
        return HttpResponse.serverError().body("{\"message\":\"Crashed\"}");
    }

    @Get(value = "/api/tcpPort", produces = MediaType.APPLICATION_JSON)
    public HttpResponse tcpPort(HttpRequest request) {
        if (!request.getHeaders().isKeepAlive()) {
            return HttpResponse.serverError().body("{\"message\":\"Should always have keep-alive\"}");
        }

        int p = request.getRemoteAddress().getPort();
        ports.add(p);
        return HttpResponse.ok().body(ports);
    }

    @Get(value = "/api/tcpPortFailed", produces = MediaType.APPLICATION_JSON)
    public HttpResponse tcpPortFailed(HttpRequest request) {
        return HttpResponse.serverError().body("{\"message\":\"Tcp Port Failed\"}");
    }

}
