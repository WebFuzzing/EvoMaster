package com.foo.rest.examples.spring.db.preparedstatement;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

public class PreparedStatementEntity {

    // unique identifier
    @Id
    @GeneratedValue
    private Long identifier;

    // integer value
    @NotNull
    private Integer integerValue;

    // string value
    @NotNull
    private String stringValue;

    // boolean value
    @NotNull
    private boolean booleanValue;

    // empty constructor
    public PreparedStatementEntity() { }

    // get method for the identifier
    public Long getIdentifier() {
        return identifier;
    }

    // set method for the identifier
    public void setIdentifier(Long identifier) {
        this.identifier = identifier;
    }

    // get method for the integerValue
    public Integer getIntegerValue() {
        return integerValue;
    }

    // set method for the integerValue
    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    // get method for the stringValue
    public String getStringValue() {
        return stringValue;
    }

    // set method for the stringValue
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    // get method for the booleanValue
    public boolean isBooleanValue() {
        return booleanValue;
    }

    // set method for the booleanValue
    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }





}
