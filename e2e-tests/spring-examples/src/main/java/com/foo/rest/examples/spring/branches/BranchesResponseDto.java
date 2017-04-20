package com.foo.rest.examples.spring.branches;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class BranchesResponseDto {

    @ApiModelProperty(value = "Result value", required = true)
    public Integer value;

    public BranchesResponseDto() {
    }
}
