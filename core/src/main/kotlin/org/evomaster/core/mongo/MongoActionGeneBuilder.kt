package org.evomaster.core.mongo

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene
import java.util.Date

class MongoActionGeneBuilder {
    fun <T> buildGene(fieldName: String, value: T): Gene {
        return when (value) {
            is Int -> IntegerGene(fieldName, min = Int.MIN_VALUE, max = Int.MAX_VALUE)
            is Long -> LongGene(fieldName, min = Long.MIN_VALUE, max = Long.MAX_VALUE)
            is Double -> DoubleGene(fieldName, min = Double.MIN_VALUE, max = Double.MAX_VALUE)
            is String -> StringGene(name = fieldName, minLength = Int.MIN_VALUE)
            is Boolean -> BooleanGene(name = fieldName)
            //is Array<*> ->  ArrayGene<*>(name = fieldName)
            //is Object -> ObjectGene
            //is Date -> DateGene
            //is null -> NullGene
            //Unhandled Types: Binary Data, Object Id, Regular Expression, Javascript, Timestamp, Decimal128, Min/max key
            else -> throw IllegalArgumentException("Cannot handle: $fieldName.")
        }
    }
}
