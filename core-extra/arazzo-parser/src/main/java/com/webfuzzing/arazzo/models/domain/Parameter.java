package com.webfuzzing.arazzo.models.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.webfuzzing.arazzo.deserializer.AnyExpressionDeserializer;

/**
 * Representing the model Parameter Object
 * Describes a single step parameter
 */
public class Parameter {
    /**
     * The name of the parameter.
     */
    private String name;

    /**
     * The location of the parameter.
     */
    private String in;

    /**
     * The value to pass in the parameter.
     */
    @JsonDeserialize(using = AnyExpressionDeserializer.class)
    private AnyExpression value;

    public Parameter() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public AnyExpression getValue() {
        return value;
    }

    public void setValue(AnyExpression value) {
        this.value = value;
    }
}
