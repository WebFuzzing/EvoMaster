package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represents a $bitsAnySet operation.
 */
public class BitsAnySetOperation extends BitsOperation {

    public BitsAnySetOperation(String fieldName, long bitmask) {
        super(fieldName, bitmask);
    }
}
