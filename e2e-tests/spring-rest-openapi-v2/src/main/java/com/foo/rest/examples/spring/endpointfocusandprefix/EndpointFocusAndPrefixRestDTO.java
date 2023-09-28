package com.foo.rest.examples.spring.endpointfocusandprefix;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class EndpointFocusAndPrefixRestDTO {

    @ApiModelProperty(value ="id", required = true)
    public int id;

    @ApiModelProperty(value = "userName", required = true)
    public String userName;

    @ApiModelProperty(value = "firstName", required = true)
    public String firstName;

    @ApiModelProperty(value = "lastName", required = true)
    public String lastName;

    public EndpointFocusAndPrefixRestDTO() { }

}
