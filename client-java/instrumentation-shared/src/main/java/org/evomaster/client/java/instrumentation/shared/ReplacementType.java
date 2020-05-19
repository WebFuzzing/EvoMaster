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
    TRACKER}
