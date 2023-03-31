package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $all operation.
 * Matches arrays that contain all elements specified in the query.
 */
open class AllOperation<V>(
    open val fieldName: String,
    open val values: ArrayList<V>
) : QueryOperation()