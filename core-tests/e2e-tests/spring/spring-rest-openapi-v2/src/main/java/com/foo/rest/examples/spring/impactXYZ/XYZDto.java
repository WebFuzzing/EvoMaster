package com.foo.rest.examples.spring.impactXYZ;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * created by manzh on 2020-06-09
 */
@ApiModel
public class XYZDto {

    @ApiModelProperty(required = true)
    public int x;

    @ApiModelProperty(required = true)
    public String y;

    @ApiModelProperty(required = false)
    public String z;

    public XYZDto(){}

    public XYZDto(int x, String y, String z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
