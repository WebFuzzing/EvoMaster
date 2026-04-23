package com.foo.rest.examples.spring.resource.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Pattern;

/** automatically created on 2019-08-29 */
@ApiModel
public class RA {
  public RA() {}

  public RA(Long id_var, String name_var, int value_var) {
    this.id = id_var;
    this.name = name_var;
    this.valueInt = value_var;
  }

  @ApiModelProperty(required = true)
  public Long id;

  @ApiModelProperty(required = true)
  @Pattern(regexp = "\\s+-\\d+-\\s+")
  public String name;

  @ApiModelProperty(required = true)
  public int valueInt;
}

