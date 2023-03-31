package org.evomaster.client.java.controller.mongo

import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.client.java.controller.mongo.operations.synthetic.*
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper
import kotlin.math.abs

class MongoHeuristicsCalculator {

    /**
     * Compute a "branch" distance heuristics.
     *
     * @param query the QUERY clause which we want to resolve as true
     * @param doc a document in the database for which we want to calculate the distance
     * @return a branch distance, where 0 means that the document would make the QUERY resolve to true
     */
    fun computeExpression(query: Bson, doc: Document): Double {
        val operation = getOperation(query)
        return calculateDistance(operation, doc)
    }

    private fun getOperation(query: Bson): QueryOperation {
        val bsonDocument = query.toBsonDocument(BsonDocument::class.java, MongoClientSettings.getDefaultCodecRegistry())
        val queryDocument = DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
        return QueryParser().parse(queryDocument)
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
            is InvertedAllOperation<*> -> calculateDistanceForInvertedAll(operation, doc)
            is SizeOperation -> calculateDistanceForSize(operation, doc)
            is InvertedSizeOperation -> calculateDistanceForInvertedSize(operation, doc)
            is ElemMatchOperation -> calculateDistanceForElemMatch(operation, doc)
            is ExistsOperation -> calculateDistanceForExists(operation, doc)
            is ModOperation -> calculateDistanceForMod(operation, doc)
            is InvertedModOperation -> calculateDistanceForInvertedMod(operation, doc)
            is NotOperation -> calculateDistanceForNot(operation, doc)
            is TypeOperation -> calculateDistanceForType(operation, doc)
            is InvertedTypeOperation -> calculateDistanceForInvertedType(operation, doc)
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
        return operation.conditions.minOf { condition -> calculateDistance(condition, doc) }
    }

    private fun calculateDistanceForAnd(operation: AndOperation, doc: Document): Double {
        return operation.conditions.sumOf { condition -> calculateDistance(condition, doc) }
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

    private fun <V> calculateDistanceForInvertedAll(operation: InvertedAllOperation<V>, doc: Document): Double {
        val expectedValues = operation.values

        return when (val actualValues = doc[operation.fieldName]) {
            is ArrayList<*> -> if(actualValues.containsAll(expectedValues)) 1.0 else 0.0
            else -> 0.0
        }
    }

    private fun calculateDistanceForSize(operation: SizeOperation, doc: Document): Double {
        val expectedSize = operation.value

        return when (val actualValue = doc[operation.fieldName]) {
            is ArrayList<*> -> abs(actualValue.size.toDouble() - expectedSize.toDouble())
            else -> Double.MAX_VALUE
        }
    }

    private fun calculateDistanceForInvertedSize(operation: InvertedSizeOperation, doc: Document): Double {
        val expectedSize = operation.value

        return when (val actualValue = doc[operation.fieldName]) {
            is ArrayList<*> -> if(actualValue.size ==  expectedSize) 1.0 else 0.0
            else -> 0.0
        }
    }

    private fun calculateDistanceForElemMatch(operation: ElemMatchOperation, doc: Document): Double {
        return when (val actualValue = doc[operation.fieldName]) {
            is ArrayList<*> -> actualValue.minOf { elem ->
                calculateDistance(operation.condition, Document().append(operation.fieldName, elem))
            }
            else -> Double.MAX_VALUE
        }
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
                abs(actualRemainder.toDouble() - expectedRemainder.toDouble())
            }

            else -> Double.MAX_VALUE
        }
    }

    private fun calculateDistanceForInvertedMod(operation: InvertedModOperation, doc: Document): Double {
        val expectedRemainder = operation.remainder

        return when (val actualValue = doc[operation.fieldName]) {
            // Change to number?
            is Int -> {
                val actualRemainder = actualValue.mod(operation.divisor)
                if(actualRemainder ==  expectedRemainder) 1.0 else 0.0
            }

            else -> 0.0
        }
    }

    private fun calculateDistanceForNot(operation: NotOperation, doc: Document): Double {
        val fieldName = operation.fieldName
        if (doc[fieldName] == null) return 0.0

        val condition = operation.condition
        val invertedOperation = invertOperation(condition)

        return calculateDistance(invertedOperation, doc)
    }

    private fun calculateDistanceForNor(operation: NorOperation, doc: Document): Double {
        return operation.conditions.sumOf { condition -> calculateDistance(invertOperation(condition), doc) }
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

    private fun calculateDistanceForInvertedType(operation: InvertedTypeOperation, doc: Document): Double {
        val field = operation.fieldName
        val expectedType = BsonTypeClassMap().get(operation.type).typeName
        val actualType = when (val value = doc[field]) {
            null -> "null"
            else -> value::class.java.typeName
        }
        // el else ??
        return if(actualType != expectedType) 0.0 else 1.0
    }

    private fun invertOperation(operation: QueryOperation): QueryOperation {
        return when (operation) {
            is EqualsOperation<*> -> NotEqualsOperation(operation.fieldName, operation.value)
            is NotEqualsOperation<*> -> EqualsOperation(operation.fieldName, operation.value)
            is GreaterThanOperation<*> -> LessThanEqualsOperation(operation.fieldName, operation.value)
            is GreaterThanEqualsOperation<*> -> LessThanOperation(operation.fieldName, operation.value)
            is LessThanOperation<*> -> GreaterThanEqualsOperation(operation.fieldName, operation.value)
            is LessThanEqualsOperation<*> -> GreaterThanOperation(operation.fieldName, operation.value)
            is NotOperation -> operation.condition
            is AllOperation<*> -> InvertedAllOperation(operation.fieldName, operation.values)
            is InvertedAllOperation<*> -> AllOperation(operation.fieldName, operation.values)
            is AndOperation -> OrOperation(operation.conditions.map { condition -> invertOperation(condition) })
            is OrOperation -> NorOperation(operation.conditions)
            is ExistsOperation -> ExistsOperation(operation.fieldName, !operation.boolean)
            is InOperation<*> -> NotInOperation(operation.fieldName, operation.values)
            is NotInOperation<*> -> InOperation(operation.fieldName, operation.values)
            is ModOperation -> InvertedModOperation(operation.fieldName, operation.divisor, operation.remainder)
            is InvertedModOperation -> ModOperation(operation.fieldName, operation.divisor, operation.remainder)
            is NorOperation -> OrOperation(operation.conditions)
            is SizeOperation -> InvertedSizeOperation(operation.fieldName, operation.value)
            is InvertedSizeOperation -> SizeOperation(operation.fieldName, operation.value)
            is TypeOperation -> InvertedTypeOperation(operation.fieldName, operation.type)
            is InvertedTypeOperation -> TypeOperation(operation.fieldName, operation.type)
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
            // Modify
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