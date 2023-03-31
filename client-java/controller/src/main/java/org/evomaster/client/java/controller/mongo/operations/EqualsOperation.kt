package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $eq operation.
 * Matches documents where the value of a field equals the specified value.
 */
class EqualsOperation<V>(fieldName: String, value: V) : ComparisonOperation<V>(fieldName, value)