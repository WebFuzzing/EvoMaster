package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $in operation.
 * Selects the documents where the value of a field equals any value in the specified array.
 */
open class InOperation<V>(open val fieldName: String, open val values: ArrayList<V>) : QueryOperation()