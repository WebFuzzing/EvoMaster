package org.evomaster.core.output.naming

/**
 *
 */
enum class NamingStrategy {

    /**
     * Standard, naive approach.
     * Each test gets a unique, incremental number
     */
    NUMBERED,

    /**
     * Apply a deterministic set of rules based on the actions' content.
     */
    DETERMINISTIC,

    /**
     * Call an LLM to create the names based on the tests' content, if available
     */
    LLM
    ;
}
