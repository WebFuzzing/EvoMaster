package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $and operation.
 * Joins query clauses with a logical AND returns all documents that match the conditions of all clauses.
 */
open class AndOperation(open val conditions: List<QueryOperation>) : QueryOperation()