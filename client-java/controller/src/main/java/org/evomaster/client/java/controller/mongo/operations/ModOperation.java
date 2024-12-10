package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $mod operation.
 * Select documents where the value of a field divided by a divisor has the specified remainder.
 */
public class ModOperation extends QueryOperation{
    private final String fieldName;
    private final Long divisor;
    private final Long remainder;

    public ModOperation(String fieldName, Long divisor, Long remainder) {
        this.fieldName = fieldName;
        this.divisor = divisor;
        this.remainder = remainder;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Long getDivisor() {
        return divisor;
    }

    public Long getRemainder() {
        return remainder;
    }
}