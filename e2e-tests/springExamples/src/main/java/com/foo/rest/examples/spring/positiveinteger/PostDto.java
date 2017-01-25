package com.foo.rest.examples.spring.positiveinteger;


//@ApiModel
public class PostDto {

//    @ApiModelProperty("Value to check")
    public Integer value;


    public PostDto() {
    }

    public PostDto(Integer value) {
        this.value = value;
    }
}
