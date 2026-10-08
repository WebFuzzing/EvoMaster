package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represents a $bitsAnyClear operation.
 */
public class BitsAnyClearOperation extends BitsOperation {

    public BitsAnyClearOperation(String fieldName, long bitmask) {
        super(fieldName, bitmask);
    }
}
