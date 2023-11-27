package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ResourceDto {

    @ApiModelProperty
    public String stringValue;

    @ApiModelProperty
    public int integerValue;

    @ApiModelProperty
    public boolean booleanValue;
}
