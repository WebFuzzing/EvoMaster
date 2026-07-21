package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $mod operation.
 * Select documents where the value of a field divided by a divisor has the specified remainder.
 */
public class ModOperation extends QueryOperationWithField {
    private final Long divisor;
    private final Long remainder;

    public ModOperation(String fieldName, Long divisor, Long remainder) {
        super(fieldName);
        this.divisor = divisor;
        this.remainder = remainder;
    }

    public Long getDivisor() {
        return divisor;
    }

    public Long getRemainder() {
        return remainder;
    }
}
