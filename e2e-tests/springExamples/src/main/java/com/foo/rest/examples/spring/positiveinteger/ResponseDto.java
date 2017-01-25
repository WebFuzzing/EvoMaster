package com.foo.rest.examples.spring.positiveinteger;


//@ApiModel
public class ResponseDto {

//    @ApiModelProperty("Whether the checked value was positive or not")
    public Boolean isPositive;


    public ResponseDto() {
    }

    public ResponseDto(Boolean isPositive) {
        this.isPositive = isPositive;
    }
}
