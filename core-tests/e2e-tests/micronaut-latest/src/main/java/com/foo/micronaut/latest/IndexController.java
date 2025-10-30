package com.foo.micronaut.latest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.validation.constraints.PositiveOrZero;
import java.util.HashSet;
import java.util.Set;

@Controller()
public class IndexController {

    private static final Set<Integer> ports = new HashSet<>();

    @Operation(summary = "Index controller to crash micronaut with 500",
            description = "To test the crash scenario."
    )
    @ApiResponse(responseCode = "500", description = "Expected outcome")
    @Get(produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> index() {
        // it is expected the application to send connection close when it crashes
        throw new TestException();
    }

    @Operation(summary = "POST Controller for test",
            description = "Return 200"
    )
    @ApiResponse(responseCode = "200", description = "Working POST route")
    @Post(value="{?x,y}", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> indexPost(@Nullable @PositiveOrZero Integer x, @Nullable @PositiveOrZero Integer y) {
        int z = ( x != null && y != null) ? x + y : 0;
        return HttpResponse.status(HttpStatus.OK).body("{\"message\":\"Working!\",\"answer\":" + z + "}");
    }

    @Get(value = "/api/tcpPort", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> tcpPort(HttpRequest request) {
        if (!request.getHeaders().isKeepAlive()) {
            return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"Should always have keep-alive\"}");
        }

        int p = request.getRemoteAddress().getPort();
        ports.add(p);
        return HttpResponse.status(HttpStatus.OK).body(ports.toString());
    }

    @Get(value = "/api/tcpPortFailed", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> tcpPortFailed(HttpRequest request) {
        return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"Tcp Port Failed\"}");
    }
}
