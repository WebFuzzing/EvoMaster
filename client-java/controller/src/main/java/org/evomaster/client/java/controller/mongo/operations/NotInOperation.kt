package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $nin operation.
 * Selects the documents where:
 *  - the field value is not in the specified array
 *  - the field does not exist.
 */
open class NotInOperation<V>(open val fieldName: String, open val values: ArrayList<V>) : QueryOperation()