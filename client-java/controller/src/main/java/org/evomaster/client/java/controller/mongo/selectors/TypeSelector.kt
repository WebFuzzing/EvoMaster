package org.evomaster.client.java.controller.mongo.selectors

import org.bson.BsonType
import org.evomaster.client.java.controller.mongo.operations.*

class TypeSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$type"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is Int -> TypeOperation(fieldName, BsonType.findByValue(value))
            is String -> TypeOperation(fieldName, BsonType.valueOf(value))
            else -> null
        }
    }
}