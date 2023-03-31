package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $nor operation.
 * Selects the documents that fail all the query expressions in the array.
 */
open class NorOperation(open val conditions: List<QueryOperation>) : QueryOperation()