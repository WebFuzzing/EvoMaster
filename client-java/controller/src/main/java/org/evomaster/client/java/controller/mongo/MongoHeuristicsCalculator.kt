package org.evomaster.client.java.controller.mongo

import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

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
            is AllOperation<*> -> calculateDistanceForAll(operation, doc)
            is SizeOperation -> calculateDistanceForSize(operation, doc)
            is ElemMatchOperation -> calculateDistanceForElemMatch(operation, doc)
            is ExistsOperation -> calculateDistanceForExists(operation, doc)
            is ModOperation -> calculateDistanceForMod(operation, doc)
            is NotOperation -> calculateDistanceForNot(operation, doc)
            else -> Double.MAX_VALUE
        }
    }

    private fun <V> calculateDistanceForEquals(operation: EqualsOperation<V>, doc: Document): Double {
        return calculateDistanceForComparisonOperation(operation, doc) { dif -> kotlin.math.abs(dif) }
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
        calculate: (Double) -> Double
    ): Double {
        val expectedValue = operation.value
        val actualValue = doc[operation.fieldName]
        return if (actualValue is Number && expectedValue is Number) {
            val difference = actualValue.toDouble() - expectedValue.toDouble()
            calculate(difference)
        } else {
            Double.MAX_VALUE
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
        val actualValue = doc[operation.fieldName]
        var min = Double.MAX_VALUE
        expectedValues.forEach { value ->
            if (actualValue is Number && value is Number) {
                val dif = kotlin.math.abs(actualValue.toDouble() - value.toDouble())
                if (dif < min) min = dif
            }
        }
        return min
    }

    private fun <V> calculateDistanceForAll(operation: AllOperation<V>, doc: Document): Double {
        val expectedValues = operation.values
        val actualValues = doc[operation.fieldName]
        val distances = expectedValues.map { value ->
            var min = Double.MAX_VALUE
            if (actualValues is List<*> && value is Number) {
                actualValues.forEach { actualValue ->
                    if (actualValue is Number) {
                        val dif = kotlin.math.abs(actualValue.toDouble() - value.toDouble())
                        if (dif < min) min = dif
                    }
                }
            }
            min
        }
        return distances.sum()
    }

    private fun calculateDistanceForSize(operation: SizeOperation, doc: Document): Double {
        val expectedValue = operation.value
        val actualValue = doc[operation.fieldName]
        val distance =
            if (actualValue is List<*>) {
                kotlin.math.abs(actualValue.size.toDouble() - expectedValue.toDouble())
            } else {
                Double.MAX_VALUE
            }
        return distance
    }

    private fun calculateDistanceForElemMatch(operation: ElemMatchOperation, doc: Document): Double {
        // NOT FINISHED, FIX
        return operation.filters.sumOf { filter -> calculateDistance(filter, doc) }
    }

    private fun calculateDistanceForExists(operation: ExistsOperation, doc: Document): Double {
        // NOT FINISHED
        // Only handling the case where boolean is true
        val fieldName = operation.fieldName
        val actualFields = doc.keys
        // Define distance between string
        return Double.MAX_VALUE
    }

    private fun calculateDistanceForMod(operation: ModOperation, doc: Document): Double {
        val actualValue = doc[operation.fieldName]
        if(actualValue is Int){
            val actualRemainder = actualValue.mod(operation.divisor)
            val expectedRemainder = operation.remainder
            return  kotlin.math.abs(actualRemainder.toDouble() - expectedRemainder.toDouble())
        }
        return Double.MAX_VALUE
    }

    private fun calculateDistanceForNot(operation: NotOperation, doc: Document): Double {
        // NOT FINISHED
        val fieldName = operation.fieldName
        val filter = operation.filter
        val actualValue = doc[fieldName]

        if (actualValue == null) return 0.0
        val negatedOperation = negateOperation(filter)
        return calculateDistance(negatedOperation, doc)
    }

    private fun calculateDistanceForNor(operation: NorOperation, doc: Document): Double {
        // NOT FINISHED
        return operation.filters.sumOf { filter -> calculateDistance(negateOperation(filter), doc)}
    }

    private fun negateOperation(operation: QueryOperation): QueryOperation {
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
}