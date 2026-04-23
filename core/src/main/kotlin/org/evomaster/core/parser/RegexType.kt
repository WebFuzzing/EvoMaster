package org.evomaster.core.parser

/**
 * We have different sources of regex, which might have different syntax and support for different operators:
 * - JS regex in (OpenAPI) schemas
 * - schemas in databases
 * - white-box taint-analysis in Java code
 * - etc.
 */
enum class RegexType {
    JVM,
    POSTGRES_LIKE,
    POSTGRES_SIMILAR_TO,
    ECMA_262
}
