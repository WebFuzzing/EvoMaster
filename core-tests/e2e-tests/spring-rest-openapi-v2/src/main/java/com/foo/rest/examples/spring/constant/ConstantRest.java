package com.foo.rest.examples.spring.constant;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/constant")
public class ConstantRest {

    @ApiOperation("Check if the given value is positiv")
    @RequestMapping(
            value = "/{value}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ConstantResponseDto checkConstant(
            @ApiParam("Value to check")
            @PathVariable("value")
            Integer value){

        ConstantResponseDto dto = new ConstantResponseDto();
        dto.ok = (value == 123);

        return dto;
    }
}
