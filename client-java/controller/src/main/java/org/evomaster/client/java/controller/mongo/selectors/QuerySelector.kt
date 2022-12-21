package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

abstract class QuerySelector {
    abstract fun getOperation(query: Document): QueryOperation?
    fun isUniqueEntry(map: Map<*, *>) = map.size == 1
}