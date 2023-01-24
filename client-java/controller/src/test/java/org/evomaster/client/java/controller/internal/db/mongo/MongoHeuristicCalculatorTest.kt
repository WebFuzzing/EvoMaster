package org.evomaster.client.java.controller.internal.mongo

import com.mongodb.client.model.Filters
import org.bson.BsonType
import org.bson.Document
import org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MongoHeuristicCalculatorTest {

    @Test
    fun testEquals() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.eq("age", 10)
        val bsonFalse = Filters.eq("age", 26)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(16.0, distanceNotMatch)
    }

    @Test
    fun testNotEquals() {
        val doc = Document().append("age", 10)
        val bsonTrue1 = Filters.ne("age", 26)
        val bsonTrue2 = Filters.ne("some-field", 26)
        val bsonFalse = Filters.ne("age", 10)
        val distanceMatch1 = MongoHeuristicsCalculator().computeExpression(bsonTrue1, doc)
        val distanceMatch2 = MongoHeuristicsCalculator().computeExpression(bsonTrue2, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch1)
        assertEquals(0.0, distanceMatch2)
        assertEquals(1.0, distanceNotMatch)
    }

    @Test
    fun testGreaterThan() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.gt("age", 5)
        val bsonFalse = Filters.gt("age", 13)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(4.0, distanceNotMatch)
    }

    @Test
    fun testGreaterThanEquals() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.gte("age", 5)
        val bsonFalse = Filters.gte("age", 13)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(3.0, distanceNotMatch)
    }

    @Test
    fun testLessThan() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.lt("age", 11)
        val bsonFalse = Filters.lt("age", 7)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(4.0, distanceNotMatch)
    }

    @Test
    fun testLessThanEquals() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.lt("age", 11)
        val bsonFalse = Filters.lt("age", 7)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(4.0, distanceNotMatch)
    }

    @Test
    fun testOr() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.or(Filters.gt("age", 9), Filters.lt("age",20))
        val bsonFalse = Filters.or(Filters.gt("age", 17), Filters.lt("age",8))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(3.0, distanceNotMatch)
    }

    @Test
    fun testAnd() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.and(Filters.gt("age", 9), Filters.lt("age",20))
        val bsonFalse = Filters.and(Filters.gt("age", 10), Filters.lt("age",8))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(4.0, distanceNotMatch)
    }

    @Test
    fun testIn() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.`in`("age", arrayListOf(1, 10, 8))
        val bsonFalse = Filters.`in`("age", arrayListOf(1, 15))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(5.0, distanceNotMatch)
    }

    @Test
    fun testNotIn() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.nin("age", arrayListOf(1, 8))
        val bsonFalse = Filters.nin("age", arrayListOf(1, 10))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(1.0, distanceNotMatch)
    }

    @Test
    fun testAll() {
        val doc = Document().append("employees", arrayListOf(1,5,6))
        val bsonTrue = Filters.all("employees", arrayListOf(1, 5, 6))
        val bsonFalse = Filters.all("employees", arrayListOf(1, 7, 8))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(3.0, distanceNotMatch)
    }

    @Test
    fun testSize() {
        val doc = Document().append("employees", listOf(1,5,6))
        val bsonTrue = Filters.size("employees", 3)
        val bsonFalse = Filters.size("employees", 5)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(2.0, distanceNotMatch)
    }

    @Test
    fun testMod() {
        val doc = Document().append("age", 20)
        val bsonTrue = Filters.mod("age", 3, 2)
        val bsonFalse = Filters.mod("age", 3, 0)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(2.0, distanceNotMatch)
    }

    @Test
    fun testNot() {
        val doc = Document().append("age", 20)
        val bsonTrue = Filters.not(Filters.gt("age", 10))
        val bsonFalse = Filters.not(Filters.gt("age", 30))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(10.0, distanceNotMatch)
    }

    @Test
    fun testExistsTrueVersion() {
        val doc = Document().append("age", 20)
        val bsonTrue = Filters.exists("age", true)
        val bsonFalse = Filters.exists("name", true)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(65563.0, distanceNotMatch)
    }

    @Test
    fun testExistsFalseVersion() {
        val doc = Document().append("age", 20)
        val bsonTrue = Filters.exists("name", false)
        val bsonFalse = Filters.exists("age", false)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(1.0, distanceNotMatch)
    }

    @Test
    fun testTypeExplicitVersion() {
        val doc = Document().append("age", 20)
        val bsonTrue = Filters.type("age", BsonType.INT32)
        val bsonFalse = Filters.type("age", BsonType.DOUBLE)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(65551.0, distanceNotMatch)
    }

    @Test
    fun testTypeAliasVersion() {
        // This is not exactly the alias. Should be?
        val doc = Document().append("age", 20)
        val bsonTrue = Filters.type("age", BsonType.INT32.name)
        val bsonFalse = Filters.type("age", BsonType.DOUBLE.name)
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(65551.0, distanceNotMatch)
    }

    /*
    @Test
    fun testElemMatch() {
        val doc = Document().append("age", 10)
        val bsonTrue = Filters.elemMatch("age", Filters.and(Filters.gt("age", 9), Filters.lt("age",20)))
        val bsonFalse = Filters.elemMatch("age", Filters.and(Filters.gt("age", 10), Filters.lt("age",8)))
        val distanceMatch = MongoHeuristicsCalculator().computeExpression(bsonTrue, doc)
        val distanceNotMatch = MongoHeuristicsCalculator().computeExpression(bsonFalse, doc)
        assertEquals(0.0, distanceMatch)
        assertEquals(10.0, distanceNotMatch)
    }
     */
}