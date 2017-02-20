package com.foo.rest.examples.spring.triangle;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.evomaster.clientJava.instrumentation.example.triangle.TriangleClassification;


@ApiModel
public class TriangleResponseDto {

    @ApiModelProperty("The classification of the 3 given edges")
    public TriangleClassification.Classification classification;
}
