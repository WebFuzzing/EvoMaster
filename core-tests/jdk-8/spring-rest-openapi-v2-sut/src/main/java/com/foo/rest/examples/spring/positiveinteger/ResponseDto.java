package com.foo.rest.examples.spring.positiveinteger;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ResponseDto {

    @ApiModelProperty("Whether the checked value was positive or not")
    public Boolean isPositive;


    public ResponseDto() {
    }

    public ResponseDto(Boolean isPositive) {
        this.isPositive = isPositive;
    }
}
