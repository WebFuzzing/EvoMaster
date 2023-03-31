package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.client.java.controller.mongo.QueryParser

/**
 * { $and: [ { <expression1> }, { <expression2> } , ... , { <expressionN> } ] }
 */
class AndSelector : MultiConditionQuerySelector() {
    override fun operator(): String = "\$and"

    override fun parseConditions(value: Any?): QueryOperation? {
        return when (value) {
            is List<*> -> {
                val conditions = value.map { condition -> QueryParser().parse( condition as Document)}
                AndOperation(conditions)
            }
            else -> null
        }
    }
}