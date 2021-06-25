package com.foo.rest.examples.spring.hypermutation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Pattern;

@ApiModel
public class HighWeightDto {

    public HighWeightDto(){}

    @ApiModelProperty(required = true)
    public Integer f1;

    @ApiModelProperty(required = true)
    public String f2;

    @ApiModelProperty(required = true)
    @Pattern(regexp = "\\d{4}-\\d{1,2}-\\d{1,2}")
    public String f3;

    @ApiModelProperty(required = true)
    public Double f4;

    @ApiModelProperty(required = true)
    public Float f5;

    @ApiModelProperty(required = true)
    public Long f6;
    
}
