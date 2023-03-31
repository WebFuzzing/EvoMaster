package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $where operation.
 * Matches documents that satisfy a JavaScript expression.
 */
open class WhereOperation (open val expression: String) : QueryOperation()