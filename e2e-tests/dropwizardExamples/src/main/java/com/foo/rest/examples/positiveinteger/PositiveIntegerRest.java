package com.foo.rest.examples.positiveinteger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Api
@Path("/pi")
public class PositiveIntegerRest {

    @ApiOperation("Check if the given value is positive")
    @Path("/{value}")
    @GET
    public ResponseDto checkIfPositive(
            @ApiParam("The value to check")
            @QueryParam("value") int value
    ){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(value);

        return dto;
    }


    @ApiOperation("Check if the given value is positive")
    @Path("/")
    @POST
    public ResponseDto checkIfPositive(PostDto postDto){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(postDto.value);

        return dto;
    }
}
