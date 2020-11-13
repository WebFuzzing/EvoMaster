package com.foo.rest.examples.spring.positiveinteger;

import com.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/pi")
public class PositiveIntegerRest {

    @ApiOperation("Check if the given value is positive")
    @RequestMapping(
            value = "/{value}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseDto checkIfPositive(
            @ApiParam("The value to check")
            @PathVariable("value") Integer value
    ){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(value);

        return dto;
    }


    @ApiOperation("Check if the given value is positive")
    @RequestMapping(
            value = "",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseDto checkIfPositive(@RequestBody PostDto postDto){

        ResponseDto dto = new ResponseDto();
        dto.isPositive = new PositiveIntegerImp().isPositive(postDto.value);

        return dto;
    }
}
