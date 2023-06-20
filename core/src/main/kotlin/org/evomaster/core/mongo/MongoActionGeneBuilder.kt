package org.evomaster.core.mongo

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.string.StringGene

class MongoActionGeneBuilder {
    fun <T> buildGene(fieldName: String, valueType: Class<T>): Gene {
        val typeName = valueType.simpleName;

        if(typeName == "String"){
            return StringGene(name = fieldName)
        }

        if(typeName == "int" || typeName == "Integer"){
            return IntegerGene(fieldName, min = Int.MIN_VALUE, max = Int.MAX_VALUE)
        }

        if(typeName == "long" || typeName == "Long"){
            return LongGene(fieldName, min = Long.MIN_VALUE, max = Long.MAX_VALUE)
        }

        if(typeName == "double" || typeName == "Double"){
            return DoubleGene(fieldName, min = Double.MIN_VALUE, max = Double.MAX_VALUE)
        }

        if(typeName == "boolean" || typeName == "Boolean") {
            return BooleanGene(name = fieldName)
        }

        if(typeName == "Date") {
            return DateGene(name = fieldName)
        }


        /*
        if(valueType == List<*>::javaClass ) {
            return ArrayGene(name = fieldName, template = IntegerGene(fieldName, min = Int.MIN_VALUE, max = Int.MAX_VALUE))
        }

         */

        //Unhandled Types: Null, Document, Binary Data, Object Id, Regular Expression, Javascript, Timestamp, Decimal128, Min/max key

        return ObjectGene(fieldName, valueType.fields.map { field -> buildGene(field.name, field.type) })

        //throw IllegalArgumentException("Cannot handle: $fieldName.")
    }
}
