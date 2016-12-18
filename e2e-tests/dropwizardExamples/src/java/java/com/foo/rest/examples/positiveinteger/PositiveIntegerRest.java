package com.foo.rest.examples.positiveinteger;

import org.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/pi")
public class PositiveIntegerRest {

    @Path("/{value}")
    @GET
    public ResponseDto checkIfPositive(
            @QueryParam("value") int value
    ){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(value);

        return dto;
    }


    @Path("/")
    @POST
    public ResponseDto checkIfPositive(PostDto postDto){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(postDto.value);

        return dto;
    }
}
