package com.foo.rest.examples.spring.positiveinteger;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class PostDto {

    @ApiModelProperty("Value to check")
    public Integer value;


    public PostDto() {
    }

    public PostDto(Integer value) {
        this.value = value;
    }
}
