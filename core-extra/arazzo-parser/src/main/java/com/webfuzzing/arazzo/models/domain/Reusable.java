package com.webfuzzing.arazzo.models.domain;

/**
 * Representing the model Reusable Object
 * A simple object to allow referencing of objects contained within the {@link Components}
 */
public class Reusable {

    public static final String REFERENCE = "reference";

    private String reference;
    private String value;

    public Reusable() {
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
