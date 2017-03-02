package com.foo.rest.examples.spring.headerlocation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class HeaderLocationDto {

    @ApiModelProperty(required = true)
    public String id;

    @ApiModelProperty
    public String value;

    public HeaderLocationDto() {
    }

    public HeaderLocationDto(String id, String value) {
        this.id = id;
        this.value = value;
    }
}
