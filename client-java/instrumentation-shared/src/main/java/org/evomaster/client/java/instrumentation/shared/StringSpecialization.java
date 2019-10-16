package org.evomaster.client.java.instrumentation.shared;

import java.io.Serializable;

/**
 * Based on taint analysis, could check how input strings are used,
 * and inform the search about it
 */
public enum StringSpecialization implements Serializable {

    /**
     * String used as a Date with unknown format
     */
    DATE_FORMAT_UNKNOWN_PATTERN,

    /**
     * String used as a Date with not explicitly supported format
     */
    DATE_FORMAT_PATTERN,

    /**
     * String used as a Date in YYYY_MM_DD format
     */
    DATE_YYYY_MM_DD,

    /**
     * String used as a Date in YYYY_MM_DD_HH_MM format
     */
    DATE_YYYY_MM_DD_HH_MM,


    /**
     * An ISO Local Date Time (i.e. ISO_LOCAL_DATE + 'T' + ISO_LOCAL_TIME)
     */
    ISO_LOCAL_DATE_TIME,

    /**
     * An ISO Local Time (with or without no seconds)
     */
    ISO_LOCAL_TIME,


    /**
     * String used as an integer
     */
    INTEGER,

    /**
     * String used with a specific, constant value
     */
    CONSTANT,

    /**
     * String used with a specific, constant value, ignoring its case
     */
    CONSTANT_IGNORE_CASE,


    /**
     * String constrained by a regular expression
     */
    REGEX,

    /**
     * String parsed to double
     */
    DOUBLE,

    /**
     * String parsed to long
     */
    LONG,

    /**
     * String parsed to boolean
     */
    BOOLEAN,

    /**
     * String parsed to float
     */
    FLOAT

}
