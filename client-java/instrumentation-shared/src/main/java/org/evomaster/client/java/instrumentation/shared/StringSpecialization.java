package org.evomaster.client.java.instrumentation.shared;

/**
 * Based on taint analysis, could check how input strings are used,
 * and inform the search about it
 */
public enum StringSpecialization {

    DATE_YYYY_MM_DD,

    INTEGER,

    CONSTANT
}
