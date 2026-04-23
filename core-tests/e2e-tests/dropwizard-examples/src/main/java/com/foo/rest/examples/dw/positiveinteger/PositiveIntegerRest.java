package com.foo.rest.examples.dw.positiveinteger;

import com.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Api
@Path("/pi")
public class PositiveIntegerRest {

    @ApiOperation("Check if the given value is positive")
    @Path("/{value}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDto checkIfPositive(
            @ApiParam("The value to check")
            @PathParam("value") Integer value
    ){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(value);

        return dto;
    }


    @ApiOperation("Check if the given value is positive")
    @Path("/")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDto checkIfPositive(PostDto postDto){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(postDto.value);

        return dto;
    }
}
