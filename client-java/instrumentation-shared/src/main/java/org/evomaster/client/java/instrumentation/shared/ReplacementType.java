package org.evomaster.client.java.instrumentation.shared;

public enum ReplacementType {
    /**
     * For methods that return boolean values
     */
    BOOLEAN,
    /**
     * For methods that that might throw exceptions, typically due to invalid
     * inputs
     */
    EXCEPTION,
    /**
     * For methods that we want to track when and how they are called, without
     * creating new testing targets for them
     */
    TRACKER,
    /**
     * For methods that return objects, and we want to search for specific returned values.
     * A typical case is NULL vs. NOT-NULL.
     * Another can be when a collection is returned (including arrays) and we want to distinguish
     * between EMPTY/NULL and NON-EMPTY
     */
    OBJECT
}
