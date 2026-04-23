package com.foo.rest.examples.spring.adaptivehypermutation.dto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Pattern;

/** automatically created on 2020-10-22 */
@ApiModel
public class Info {
  public Info() {}

  public Info(Integer c, String t, String d1, String d2, String d3) {
    this.c = c;
    this.t = t;
    this.d1 = d1;
    this.d2 = d2;
    this.d3 = d3;
  }

  @ApiModelProperty(required = true)
  public Integer c;

  @ApiModelProperty(required = true)
  @Pattern(regexp = "\\d{4}-\\d{1,2}-\\d{1,2}")
  public String t;

  @ApiModelProperty(required = false)
  public String d1;

  @ApiModelProperty(required = false)
  public String d2;

  @ApiModelProperty(required = false)
  public String d3;
}
