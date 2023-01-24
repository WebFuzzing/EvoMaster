package org.evomaster.client.java.controller.mongo

import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper
import org.evomaster.core.mongo.QueryParser
import kotlin.math.abs

class MongoHeuristicsCalculator {

    fun computeExpression(query: Bson, doc: Document): Double {
        val bsonDocument = query.toBsonDocument(BsonDocument::class.java, MongoClientSettings.getDefaultCodecRegistry())
        val queryDocument = DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
        val operation = QueryParser().parse(queryDocument)
        return calculateDistance(operation, doc)
    }

    private fun calculateDistance(operation: QueryOperation, doc: Document): Double {
        return when (operation) {
            is EqualsOperation<*> -> calculateDistanceForEquals(operation, doc)
            is NotEqualsOperation<*> -> calculateDistanceForNotEquals(operation, doc)
            is GreaterThanOperation<*> -> calculateDistanceForGreaterThan(operation, doc)
            is GreaterThanEqualsOperation<*> -> calculateDistanceForGreaterEqualsThan(operation, doc)
            is LessThanOperation<*> -> calculateDistanceForLessThan(operation, doc)
            is LessThanEqualsOperation<*> -> calculateDistanceForLessEqualsThan(operation, doc)
            is AndOperation -> calculateDistanceForAnd(operation, doc)
            is OrOperation -> calculateDistanceForOr(operation, doc)
            is NorOperation -> calculateDistanceForNor(operation, doc)
            is InOperation<*> -> calculateDistanceForIn(operation, doc)
            is NotInOperation<*> -> calculateDistanceForNotIn(operation, doc)
            is AllOperation<*> -> calculateDistanceForAll(operation, doc)
            is SizeOperation -> calculateDistanceForSize(operation, doc)
            is ElemMatchOperation -> calculateDistanceForElemMatch(operation, doc)
            is ExistsOperation -> calculateDistanceForExists(operation, doc)
            is ModOperation -> calculateDistanceForMod(operation, doc)
            is NotOperation -> calculateDistanceForNot(operation, doc)
            is TypeOperation -> calculateDistanceForType(operation, doc)
            else -> Double.MAX_VALUE
        }
    }

    private fun <V> calculateDistanceForEquals(operation: EqualsOperation<V>, doc: Document): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> abs(dif) }
    }

    private fun <V> calculateDistanceForNotEquals(operation: NotEqualsOperation<V>, doc: Document): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> if (dif != 0.0) 0.0 else 1.0 }
    }

    private fun <V> calculateDistanceForGreaterThan(operation: GreaterThanOperation<V>, doc: Document): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> if (dif > 0) 0.0 else 1.0 - dif }
    }

    private fun <V> calculateDistanceForGreaterEqualsThan(
        operation: GreaterThanEqualsOperation<V>,
        doc: Document
    ): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> if (dif >= 0) 0.0 else -dif }
    }

    private fun <V> calculateDistanceForLessThan(operation: LessThanOperation<V>, doc: Document): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> if (dif < 0) 0.0 else 1.0 + dif }
    }

    private fun <V> calculateDistanceForLessEqualsThan(operation: LessThanEqualsOperation<V>, doc: Document): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> if (dif <= 0) 0.0 else dif }
    }

    private fun <V> calculateDistanceForComparisonOperation(
        operation: ComparisonOperation<V>,
        doc: Document,
        calculateDistance: (Double) -> Double
    ): Double {
        val expectedValue = operation.value
        val field = operation.fieldName

        if (!documentContainsField(doc, field)) {
            return if (operation is NotEqualsOperation<V>) 0.0 else 1.0
        }

        val actualValue = doc[operation.fieldName]

        return when (val dif = compareValues(actualValue, expectedValue)) {
            null -> Double.MAX_VALUE
            else -> calculateDistance(dif)
        }
    }

    private fun calculateDistanceForOr(operation: OrOperation, doc: Document): Double {
        return operation.filters.minOf { filter -> calculateDistance(filter, doc) }
    }

    private fun calculateDistanceForAnd(operation: AndOperation, doc: Document): Double {
        return operation.filters.sumOf { filter -> calculateDistance(filter, doc) }
    }

    private fun <V> calculateDistanceForIn(operation: InOperation<V>, doc: Document): Double {
        val expectedValues = operation.values

        return when (val actualValue = doc[operation.fieldName]) {
            is ArrayList<*> -> expectedValues.minOf { value -> distanceToClosestElem(actualValue, value) }
            else -> {
                distanceToClosestElem(expectedValues, actualValue)
            }
        }
    }

    private fun <V> calculateDistanceForNotIn(operation: NotInOperation<V>, doc: Document): Double {
        val unexpectedValues = operation.values

        if (!documentContainsField(doc, operation.fieldName)) return 0.0

        val actualValue = doc[operation.fieldName]
        val hasUnexpectedElement =
            unexpectedValues.any { value -> compareValues(actualValue, value) == 0.0 }

        return if (hasUnexpectedElement) 1.0 else 0.0
    }

    private fun <V> calculateDistanceForAll(operation: AllOperation<V>, doc: Document): Double {
        val expectedValues = operation.values

        return when (val actualValues = doc[operation.fieldName]) {
            is ArrayList<*> -> expectedValues.sumOf { value -> distanceToClosestElem(actualValues, value) }
            else -> Double.MAX_VALUE
        }
    }

    private fun calculateDistanceForSize(operation: SizeOperation, doc: Document): Double {
        val expectedValue = operation.value

        return when (val actualValue = doc[operation.fieldName]) {
            is List<*> -> abs(actualValue.size.toDouble() - expectedValue.toDouble())
            else -> Double.MAX_VALUE
        }
    }

    private fun calculateDistanceForElemMatch(operation: ElemMatchOperation, doc: Document): Double {
        // NOT FINISHED, FIX
        return operation.filters.sumOf { filter -> calculateDistance(filter, doc) }
    }

    private fun calculateDistanceForExists(operation: ExistsOperation, doc: Document): Double {
        val expectedField = operation.fieldName
        val actualFields = doc.keys

        return when (operation.boolean) {
            true -> actualFields.minOf { field -> DistanceHelper.getLeftAlignmentDistance(field, expectedField) }
                .toDouble()

            else -> {
                // 1.0 or MAX_VALUE?
                if (!documentContainsField(doc, expectedField)) 0.0 else 1.0
            }
        }
    }

    private fun calculateDistanceForMod(operation: ModOperation, doc: Document): Double {
        val expectedRemainder = operation.remainder

        return when (val actualValue = doc[operation.fieldName]) {
            // Change to number?
            is Int -> {
                val actualRemainder = actualValue.mod(operation.divisor)
                return abs(actualRemainder.toDouble() - expectedRemainder.toDouble())
            }

            else -> Double.MAX_VALUE
        }
    }

    private fun calculateDistanceForNot(operation: NotOperation, doc: Document): Double {
        val fieldName = operation.fieldName
        if (doc[fieldName] == null) return 0.0

        val filter = operation.filter
        val invertedOperation = invertOperation(filter)

        return calculateDistance(invertedOperation, doc)
    }

    private fun calculateDistanceForNor(operation: NorOperation, doc: Document): Double {
        // NOT FINISHED. Must include cases where field is not defined.
        return operation.filters.sumOf { filter -> calculateDistance(invertOperation(filter), doc) }
    }

    private fun calculateDistanceForType(operation: TypeOperation, doc: Document): Double {
        val field = operation.fieldName
        val expectedType = BsonTypeClassMap().get(operation.type).typeName
        val actualType = when (val value = doc[field]) {
            null -> "null"
            else -> value::class.java.typeName
        }

        return DistanceHelper.getLeftAlignmentDistance(actualType, expectedType).toDouble()
    }

    private fun invertOperation(operation: QueryOperation): QueryOperation {
        // NOT FINISHED
        return when (operation) {
            is EqualsOperation<*> -> NotEqualsOperation(operation.fieldName, operation.value)
            is NotEqualsOperation<*> -> EqualsOperation(operation.fieldName, operation.value)
            is GreaterThanOperation<*> -> LessThanEqualsOperation(operation.fieldName, operation.value)
            is GreaterThanEqualsOperation<*> -> LessThanOperation(operation.fieldName, operation.value)
            is LessThanOperation<*> -> GreaterThanEqualsOperation(operation.fieldName, operation.value)
            is LessThanEqualsOperation<*> -> GreaterThanOperation(operation.fieldName, operation.value)
            else -> operation
        }
    }

    private fun <T1, T2> compareValues(val1: T1, val2: T2): Double? {

        if (val1 is Double && val2 is Double) {
            return val1 - val2
        }

        if (val1 is Long && val2 is Long) {
            return (val1 - val2).toDouble()
        }

        if (val1 is Int && val2 is Int) {
            return (val1 - val2).toDouble()
        }

        if (val1 is String && val2 is String) {
            return DistanceHelper.getLeftAlignmentDistance(val1, val2).toDouble()
        }

        if (val1 is ArrayList<*> && val2 is ArrayList<*>) {
            return 1.0
        }

        return null
    }

    private fun documentContainsField(doc: Document, field: String) = doc.keys.contains(field)

    private fun distanceToClosestElem(list: ArrayList<*>, value: Any?): Double {
        var minDist = Double.MAX_VALUE

        list.forEach { elem ->
            val dif = compareValues(elem, value)
            if (dif != null) {
                val absDif = abs(dif)
                if (absDif < minDist) minDist = absDif
            }
        }

        return minDist
    }
}