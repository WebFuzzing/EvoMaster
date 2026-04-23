package com.foo.rest.examples.spring.constant;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ConstantResponseDto {

    @ApiModelProperty
    public Boolean ok;
}
