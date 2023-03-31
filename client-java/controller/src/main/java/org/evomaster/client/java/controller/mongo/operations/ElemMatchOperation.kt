package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $elemMatch operation.
 * Selects documents if element in the array field matches all the specified $elemMatch conditions.
 * Here it only has one condition to match implementation in "com.mongodb.client.model.Filters"
 */
open class ElemMatchOperation(open val fieldName: String, open val condition: QueryOperation) : QueryOperation()