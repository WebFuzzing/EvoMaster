package com.foo.rest.examples.spring.namedresource;

/**
 * Created by arcand on 01.03.17.
 */
public class NamedResourceDto {

    public String name;

    public String value;

    public NamedResourceDto() {
    }

    public NamedResourceDto(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
