package com.foo.rest.examples.spring.adaptivehypermutation.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/** automatically created on 2020-10-22 */
@ApiModel
public class Bar {
  public Bar() {}

  public Bar(Integer id_var, String b, Integer c) {
    this.a = id_var;
    this.b = b;
    this.c = c;
  }

  @ApiModelProperty(required = true)
  public Integer a;

  @ApiModelProperty(required = false)
  public String b;

  @ApiModelProperty(required = false)
  public Integer c;
}
