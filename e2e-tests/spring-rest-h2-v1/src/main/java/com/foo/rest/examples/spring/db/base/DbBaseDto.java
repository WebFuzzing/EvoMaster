package com.foo.rest.examples.spring.db.base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DbBaseDto {

    @ApiModelProperty
    public Long id;

    @ApiModelProperty
    public String name;

    public DbBaseDto(){}

    public DbBaseDto(Long id, String name){
        this.id = id;
        this.name = name;
    }
}
