package com.foo.rest.examples.spring.impact;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * created by manzh on 2019-09-12
 */
@ApiModel
public class NoImpactIntFieldsDto {

    @ApiModelProperty(required = true)
    public String name;

    @ApiModelProperty(required = true)
    public int impactIntField;

    @ApiModelProperty(required = true)
    public int noImpactIntField;

    public NoImpactIntFieldsDto() {
    }

    public NoImpactIntFieldsDto(String name, int impactIntField, int noImpactIntField){
        this.name = name;
        this.impactIntField = impactIntField;
        this.noImpactIntField = noImpactIntField;
    }
}
