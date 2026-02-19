package org.evomaster.core.problem.rest.oracle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HttpSemanticsOracleTest {

    @Test
    fun testUnchangedModifiedFieldReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","ts":"2026-01-01"}""",
            bodyAfter  = """{"name":"Doe","ts":"2026-01-02"}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testNoModifiedFieldChangedReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","email":"a@a.com","age":30}""",
            bodyAfter  = """{"name":"Doe","email":"a@a.com","age":31}""",
            fieldNames = setOf("name", "email")
        ))
    }

    @Test
    fun testModifiedFieldAbsentInBothBodiesReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"age":30}""",
            bodyAfter  = """{"age":31}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testUnchangedIntegerModifiedFieldReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"count":42,"label":"test"}""",
            bodyAfter  = """{"count":42,"label":"changed"}""",
            fieldNames = setOf("count")
        ))
    }

    // hasChangedModifiedFields — field changed -> true
    @Test
    fun testChangedModifiedFieldReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","age":42}""",
            bodyAfter  = """{"name":"Bob","age":42}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testOneOfMultipleModifiedFieldsChangedReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","email":"a@a.com","age":42}""",
            bodyAfter  = """{"name":"Doe","email":"b@b.com","age":42}""",
            fieldNames = setOf("name", "email")
        ))
    }

    @Test
    fun testModifiedFieldPresentInBeforeButAbsentInAfterReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe"}""",
            bodyAfter  = """{"age":42}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testChangedIntegerModifiedFieldReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"count":42,"label":"test"}""",
            bodyAfter  = """{"count":44,"label":"test"}""",
            fieldNames = setOf("count")
        ))
    }

    @Test
    fun testInvalidJsonDifferentBodiesFallbackReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid json",
            bodyAfter  = "also not valid json",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testInvalidJsonSameBodiesFallbackReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid json",
            bodyAfter  = "not valid json",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testJsonArrayDifferentBodiesFallbackReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """[{"name":"Doe"}]""",
            bodyAfter  = """[{"name":"Bob"}]""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testJsonArraySameBodiesFallbackReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """[{"name":"Doe"}]""",
            bodyAfter  = """[{"name":"Doe"}]""",
            fieldNames = setOf("name")
        ))
    }
}
