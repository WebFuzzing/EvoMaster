package org.evomaster.client.java.instrumentation.shared;

import java.io.Serializable;

/**
 * Based on taint analysis, could check how input strings are used,
 * and inform the search about it
 */
public enum StringSpecialization implements Serializable {

    /**
     * String used as a Data in YYYY_MM_DD format
     */
    DATE_YYYY_MM_DD,

    /**
     * String used as an integer
     */
    INTEGER,

    /**
     * String used with a specific, constant value
     */
    CONSTANT
}
