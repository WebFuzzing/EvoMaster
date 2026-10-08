package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represents a $bitsAllClear operation.
 */
public class BitsAllClearOperation extends BitsOperation {

    public BitsAllClearOperation(String fieldName, long bitmask) {
        super(fieldName, bitmask);
    }
}
