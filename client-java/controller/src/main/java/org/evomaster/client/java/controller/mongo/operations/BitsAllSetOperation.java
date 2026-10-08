package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represents a $bitsAllSet operation.
 */
public class BitsAllSetOperation extends BitsOperation {

    public BitsAllSetOperation(String fieldName, long bitmask) {
        super(fieldName, bitmask);
    }
}
