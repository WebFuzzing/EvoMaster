package org.evomaster.client.java.controller.mongo.operations

open class TextOperation(
    open val search: String,
    open val language: String,
    open val caseSensitive: Boolean,
    open val diacriticSensitive: Boolean
) : QueryOperation()