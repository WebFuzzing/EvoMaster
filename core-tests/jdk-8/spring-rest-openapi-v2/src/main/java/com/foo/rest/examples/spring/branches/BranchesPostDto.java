package com.foo.rest.examples.spring.branches;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class BranchesPostDto {

    @ApiModelProperty(value = "x value", required = true)
    public Integer x;

    @ApiModelProperty(value = "y value", required = true)
    public Integer y;

    public BranchesPostDto(){}

    public BranchesPostDto(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
}
