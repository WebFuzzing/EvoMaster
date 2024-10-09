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
     * String constrained by a regular expression.
     * Should match whole text
     */
    REGEX_WHOLE,

    /**
     * String constrained by a regular expression.
     * Should match a part of the text, and not necessarely all of it
     */
    REGEX_PARTIAL,


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
    FLOAT,

    /**
     *  String should be equal to another string variable,
     *  ie 2 (or more) different variables should be keep their
     *  value in sync
     */
    EQUAL,

    /**
     * A 128-bit Universal Unique Identifier (UUID)
     */
    UUID,

    /**
     *  String should be a valid URL.
     *  All valid URLs are valid URIs, but vice-versa is not true
     */
    URL,

    /**
     *  String should be a valid URI
     */
    URI,

    /**
     * The string should represent the content of a valid JSON Object, ie {key:value,...}.
     * Note that strings are valid JSON Elements, but not Objects
     */
    JSON_OBJECT,

    /**
     * The string is representing a valid JSON array, ie [...,...,...]
     * Note that, at this point, we might have no info on the internal structure of its elements,
     * which are handled as generic maps.
     * Those might be marshalled later, in a so called 2-phase parsing, eg see Jackson convertValue()
     */
    JSON_ARRAY,

    /**
     * The string is representing a valid JSON Map, ie {}
     *
     * JSON has no concept of Map. It has Object, where fields can be dynamically changed.
     * Its "keys" are always strings (you cannot have a Map of integers in JSON for example)
     */
    JSON_MAP,

    /**
     * Representing that a tainted JSON Map has a field of the given name
     */
    JSON_MAP_FIELD,

    /**
     * The tainted string is not supposed to be a string, but rather a different type.
     * Note that
     * "42"
     * is a string, whereas
     * 42
     * is NOT a string.
     */
    CAST_TO_TYPE

    ;

    public boolean isRegex(){
        return this == REGEX_PARTIAL || this == REGEX_WHOLE;
    }
}
