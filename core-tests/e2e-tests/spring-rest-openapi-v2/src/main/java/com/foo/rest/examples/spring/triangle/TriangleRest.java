package com.foo.rest.examples.spring.triangle;


import com.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/triangle")
public class TriangleRest {


    @ApiOperation("Check the triangle type of the given three edges")
    @RequestMapping(
            value = "/{a}/{b}/{c}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public TriangleResponseDto checkTriangle(
            @ApiParam("First edge")
            @PathVariable("a") Integer a,
            @ApiParam("Second edge")
            @PathVariable("b") Integer b,
            @ApiParam("Third edge")
            @PathVariable("c") Integer c
    ){

        TriangleResponseDto dto = new TriangleResponseDto();
        dto.classification = new TriangleClassificationImpl().classify(a,b,c);

        return dto;
    }
}
