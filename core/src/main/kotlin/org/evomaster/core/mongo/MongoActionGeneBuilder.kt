package org.evomaster.core.mongo

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Default Mapping of Java Classes to BSON types:
// https://mongodb.github.io/mongo-java-driver/3.6/javadoc/?org/bson/codecs/BsonTypeClassMap.html
class MongoActionGeneBuilder {
    private val log: Logger = LoggerFactory.getLogger(MongoActionGeneBuilder::class.java)

    fun <T> buildGene(fieldName: String, valueType: Class<T>): Gene? {
        val typeName = valueType.name;

        if(typeName == "java.lang.String"){
            return StringGene(name = fieldName)
        }

        if(typeName == "int" || typeName == "java.lang.Integer"){
            return IntegerGene(name = fieldName, min = Int.MIN_VALUE, max = Int.MAX_VALUE)
        }

        if(typeName == "long" || typeName == "java.lang.Long"){
            return LongGene(name = fieldName, min = Long.MIN_VALUE, max = Long.MAX_VALUE)
        }

        if(typeName == "double" || typeName == "java.lang.Double"){
            return DoubleGene(name = fieldName, min = Double.MIN_VALUE, max = Double.MAX_VALUE)
        }

        if(typeName == "boolean" || typeName == "java.lang.Boolean") {
            return BooleanGene(name = fieldName)
        }

        if(typeName == "java.util.Date") {
            return DateGene(name = fieldName, onlyValidDates = true)
        }

        if(isAListImplementation(valueType)){
            val elementsGene = buildGene("", valueType.componentType)
            return if(elementsGene != null) ArrayGene(name = fieldName, template = elementsGene) else null
        }

        if(typeName == "org.bson.types.Decimal128") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.Binary") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.ObjectId") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.RegularExpression") {
            return unhandledValueType(fieldName)
        }

        // Deprecated
        if(typeName == "org.bson.types.Symbol") {
            return unhandledValueType(fieldName)
        }

        // Deprecated
        if(typeName == "org.bson.types.DBPointer") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.MaxKey") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.MinKey") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.Code") {
            return unhandledValueType(fieldName)
        }

        // Deprecated
        if(typeName == "org.bson.types.CodeWithScope") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.types.BSONTimestamp") {
            return unhandledValueType(fieldName)
        }

        // Deprecated
        if(typeName == "org.bson.types.Undefined") {
            return unhandledValueType(fieldName)
        }

        if(typeName == "org.bson.Document") {
            return ObjectGene(fieldName, listOf())
        }

        return ObjectGene(fieldName, valueType.fields.mapNotNull { field -> buildGene(field.name, field.type) })
    }

    private fun unhandledValueType(fieldName: String): Gene? {
        log.warn(("Cannot convert field: $fieldName to gene"))
        return null
    }

    private fun <T> isAListImplementation(valueType: Class<T>) = valueType.interfaces.any { i -> i.typeName == "java.util.List" }
}
