package com.foo.rest.examples.spring.sqloutput;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DbTableDto {

    @ApiModelProperty
    public Long id;

    @ApiModelProperty
    public String name;

    public DbTableDto(){}

    public DbTableDto(Long id, String name){
        this.id = id;
        this.name = name;
    }
}
