package org.evomaster.client.java.controller.mongo.operations

import java.util.regex.Pattern

/**
 * Represent $regex operation.
 * Provides regular expression capabilities for pattern matching strings in queries.
 */
open class RegexOperation (open val pattern: Pattern, open val options: Array<Char>) : QueryOperation()