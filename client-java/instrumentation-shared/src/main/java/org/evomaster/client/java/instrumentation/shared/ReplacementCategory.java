package org.evomaster.client.java.instrumentation.shared;

/**
 * Each method replacement is assigned one category.
 * Categories are only used to filter out replacements to skip.
 * By default, we use all replacements, but we might want to skip some for experiments,
 * or for when they are still under development / testing.
 *
 * This enum can be extended with new entries for future papers, when introducing new
 * replacements to experiment with
 */
public enum ReplacementCategory {


    /**
     * The original set of replacements defined in the TOSEM'21 paper
     */
    BASE,

    /**
     * Replacements to handle SQL command interceptions
     */
    SQL,

    /**
     * Replacements added after BASE
     */
    EXT_0,

    /**
     * Replacements specific to handle mocking of external services
     */
    NET,

    /**
     * Replacements to handle MONGO command interceptions
     */
    MONGO

}
