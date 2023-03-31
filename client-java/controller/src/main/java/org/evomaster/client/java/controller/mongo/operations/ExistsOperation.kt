package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $exists operation.
 * When <boolean> is true, $exists matches the documents that contain the field.
 * When <boolean> is false, the query returns only the documents that do not contain the field.
 */
open class ExistsOperation(
    open val fieldName: String,
    open val boolean: Boolean
) : QueryOperation()