package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $or operation.
 * Selects the documents that satisfy at least one of the conditions.
 */
open class OrOperation(open val conditions: List<QueryOperation>) : QueryOperation()