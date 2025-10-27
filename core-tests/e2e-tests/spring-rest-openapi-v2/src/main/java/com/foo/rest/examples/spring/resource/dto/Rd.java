package com.foo.rest.examples.spring.resource.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/** automatically created on 2019-08-29 */
@ApiModel
public class Rd {
  public Rd() {}

  public Rd(Long id_var, String name_var, int value_var) {
    this.id = id_var;
    this.name = name_var;
    this.valueInt = value_var;
  }

  @ApiModelProperty(required = true)
  public Long id;

  @ApiModelProperty(required = true)
  public String name;

  @ApiModelProperty(required = true)
  public int valueInt;
}

