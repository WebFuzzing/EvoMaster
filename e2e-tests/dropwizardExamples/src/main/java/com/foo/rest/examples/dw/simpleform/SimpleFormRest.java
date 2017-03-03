package com.foo.rest.examples.dw.simpleform;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Api
@Path("/")
public class SimpleFormRest {

    @ApiOperation("create")
    @POST
    public Response post(
            @ApiParam(required = true)
            @FormParam("x")
                    Integer x,
            @ApiParam(required = true)
            @FormParam("y")
                    Integer y
    ) {

        if (x < 0 && y > 1) {
            return Response.status(200).build();
        } else {
            return Response.status(400).build();
        }
    }
}
