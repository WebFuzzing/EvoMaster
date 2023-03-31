package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.client.java.controller.mongo.QueryParser

/**
 * { $or: [ { <expression1> }, { <expression2> }, ... , { <expressionN> } ] }
 */
class OrSelector : MultiConditionQuerySelector() {
    override fun operator(): String = "\$or"

    override fun parseConditions(value: Any?): QueryOperation? {
        return when (value) {
            is List<*> -> {
                val conditions = value.map { condition -> QueryParser().parse(condition as Document)}
                OrOperation(conditions)
            }
            else -> null
        }
    }
}