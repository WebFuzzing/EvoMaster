package com.foo.rest.examples.spring.namedresource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by arcand on 01.03.17.
 */
@ApiModel
public class NamedResourceDto {

    @ApiModelProperty
    public String name;

    @ApiModelProperty
    public String value;

    public NamedResourceDto() {
    }

    public NamedResourceDto(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
