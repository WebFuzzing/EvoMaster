package com.foo.rest.examples.spring.adaptivehypermutation.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/** automatically created on 2020-10-22 */
@ApiModel
public class Foo {
  public Foo() {}

  public Foo(Integer x, String y, Info z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @ApiModelProperty(required = true)
  public Integer x;

  @ApiModelProperty(required = true)
  public String y;

  @ApiModelProperty(required = true)
  public Info z;
}
