package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

public class ACDeletePutDto {

    public String stringValue;

    public Integer integerValue;

    public Boolean booleanValue;

    public ACDeletePutDto(){}

    public ACDeletePutDto(String stringValue, Integer integerValue, Boolean booleanValue) {

        this.stringValue = stringValue;
        this.integerValue = integerValue;
        this.booleanValue = booleanValue;
    }
}
