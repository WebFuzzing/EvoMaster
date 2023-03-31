package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $text operation.
 * Performs a text search on the content of the fields indexed with a text index.
 */
open class TextOperation(
    open val search: String,
    open val language: String,
    open val caseSensitive: Boolean,
    open val diacriticSensitive: Boolean
) : QueryOperation()