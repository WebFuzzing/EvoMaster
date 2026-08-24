package org.evomaster.client.java.controller.mongo.operations;

/**
 * Base class for bitwise query operations.
 */
public abstract class BitsOperation extends QueryOperationWithField {

    private final long bitmask;

    protected BitsOperation(String fieldName, long bitmask) {
        super(fieldName);
        this.bitmask = bitmask;
    }

    public long getBitmask() {
        return bitmask;
    }
}
