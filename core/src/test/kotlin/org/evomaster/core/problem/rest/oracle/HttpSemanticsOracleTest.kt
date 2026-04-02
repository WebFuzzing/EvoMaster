package org.evomaster.core.problem.rest.oracle

import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HttpSemanticsOracleTest {

    private fun xmlBodyParam() = BodyParam(
        BooleanGene("body"),
        EnumGene("body", listOf("application/xml"))
    )

    private fun formBodyParam() = BodyParam(
        BooleanGene("body"),
        EnumGene("body", listOf("application/x-www-form-urlencoded"))
    )

    @Test
    fun testUnchangedModifiedFieldReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","ts":"2026-01-01"}""",
            bodyAfter  = """{"name":"Doe","ts":"2026-01-02"}""",
            bodyModify  = """{"name":"Test"}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testNoModifiedFieldChangedReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","email":"a@a.com","age":30}""",
            bodyAfter  = """{"name":"Doe","email":"a@a.com","age":31}""",
            bodyModify  = """{"age":31}""",
            fieldNames = setOf("name", "email")
        ))
    }

    @Test
    fun testModifiedFieldAbsentInBothBodiesReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"age":30}""",
            bodyAfter  = """{"age":31}""",
            bodyModify  = """{"age":31}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testUnchangedIntegerModifiedFieldReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"count":42,"label":"test"}""",
            bodyAfter  = """{"count":42,"label":"changed"}""",
            bodyModify  = """{"count":42,"label":"changed"}""",
            fieldNames = setOf("count")
        ))
    }

    // hasChangedModifiedFields — field changed -> true
    @Test
    fun testChangedModifiedFieldReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","age":42}""",
            bodyAfter  = """{"name":"Bob","age":42}""",
            bodyModify  = """{"name":"Bob"}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testOneOfMultipleModifiedFieldsChangedReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","email":"a@a.com","age":42}""",
            bodyAfter  = """{"name":"Doe","email":"b@b.com","age":42}""",
            bodyModify  = """{"name":"Doe","email":"b@b.com","age":42}""",
            fieldNames = setOf("name", "email")
        ))
    }

    @Test
    fun testModifiedFieldPresentInBeforeButAbsentInAfterReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe"}""",
            bodyAfter  = """{"age":42}""",
            bodyModify  = """{"age":42}""",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testChangedIntegerModifiedFieldReturnsTrue() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"count":42,"label":"test"}""",
            bodyAfter  = """{"count":44,"label":"test"}""",
            bodyModify  = """{"count":44,"label":"test"}""",
            fieldNames = setOf("count")
        ))
    }

    @Test
    fun testInvalidJsonDifferentBodiesFallbackReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid json",
            bodyAfter  = "also not valid json",
            bodyModify  = "{}",
            fieldNames = setOf("name")
        ))
    }

    @Test
    fun testInvalidJsonSameBodiesFallbackReturnsFalse() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid json",
            bodyAfter  = "not valid json",
            bodyModify  = "{}",
            fieldNames = setOf("name")
        ))
    }

    // -------------------------------------------------------------------------
    // XML variants
    // -------------------------------------------------------------------------

    @Test
    fun testUnchangedModifiedFieldReturnsFalse_Xml() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><name>Doe</name><ts>2026-01-01</ts></root>",
            bodyAfter  = "<root><name>Doe</name><ts>2026-01-02</ts></root>",
            bodyModify = "<root><name>Test</name></root>",
            fieldNames = setOf("name"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testNoModifiedFieldChangedReturnsFalse_Xml() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><name>Doe</name><email>a@a.com</email><age>30</age></root>",
            bodyAfter  = "<root><name>Doe</name><email>a@a.com</email><age>31</age></root>",
            bodyModify = "<root><age>31</age></root>",
            fieldNames = setOf("name", "email"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testModifiedFieldAbsentInBothBodiesReturnsFalse_Xml() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><age>30</age></root>",
            bodyAfter  = "<root><age>31</age></root>",
            bodyModify = "<root><age>31</age></root>",
            fieldNames = setOf("name"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testUnchangedIntegerModifiedFieldReturnsFalse_Xml() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><count>42</count><label>test</label></root>",
            bodyAfter  = "<root><count>42</count><label>changed</label></root>",
            bodyModify = "<root><count>42</count><label>changed</label></root>",
            fieldNames = setOf("count"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testChangedModifiedFieldReturnsTrue_Xml() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><name>Doe</name><age>42</age></root>",
            bodyAfter  = "<root><name>Bob</name><age>42</age></root>",
            bodyModify = "<root><name>Bob</name></root>",
            fieldNames = setOf("name"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testOneOfMultipleModifiedFieldsChangedReturnsTrue_Xml() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><name>Doe</name><email>a@a.com</email><age>42</age></root>",
            bodyAfter  = "<root><name>Doe</name><email>b@b.com</email><age>42</age></root>",
            bodyModify = "<root><name>Doe</name><email>b@b.com</email><age>42</age></root>",
            fieldNames = setOf("name", "email"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testModifiedFieldPresentInBeforeButAbsentInAfterReturnsTrue_Xml() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><name>Doe</name></root>",
            bodyAfter  = "<root><age>42</age></root>",
            bodyModify = "<root><age>42</age></root>",
            fieldNames = setOf("name"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testChangedIntegerModifiedFieldReturnsTrue_Xml() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "<root><count>42</count><label>test</label></root>",
            bodyAfter  = "<root><count>44</count><label>test</label></root>",
            bodyModify = "<root><count>44</count><label>test</label></root>",
            fieldNames = setOf("count"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testInvalidXmlDifferentBodiesFallbackReturnsFalse_Xml() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid xml",
            bodyAfter  = "also not valid xml",
            bodyModify = "<root></root>",
            fieldNames = setOf("name"),
            bodyParam  = xmlBodyParam()
        ))
    }

    @Test
    fun testInvalidXmlSameBodiesFallbackReturnsFalse_Xml() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid xml",
            bodyAfter  = "not valid xml",
            bodyModify = "<root></root>",
            fieldNames = setOf("name"),
            bodyParam  = xmlBodyParam()
        ))
    }

    // -------------------------------------------------------------------------
    // Form (application/x-www-form-urlencoded) variants
    // bodyBefore / bodyAfter are JSON GET responses; bodyModify is form-encoded
    // -------------------------------------------------------------------------

    @Test
    fun testUnchangedModifiedFieldReturnsFalse_Form() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","ts":"2026-01-01"}""",
            bodyAfter  = """{"name":"Doe","ts":"2026-01-02"}""",
            bodyModify = "name=Test",
            fieldNames = setOf("name"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testNoModifiedFieldChangedReturnsFalse_Form() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","email":"a@a.com","age":30}""",
            bodyAfter  = """{"name":"Doe","email":"a@a.com","age":31}""",
            bodyModify = "age=31",
            fieldNames = setOf("name", "email"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testModifiedFieldAbsentInBothBodiesReturnsFalse_Form() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"age":30}""",
            bodyAfter  = """{"age":31}""",
            bodyModify = "age=31",
            fieldNames = setOf("name"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testUnchangedIntegerModifiedFieldReturnsFalse_Form() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"count":42,"label":"test"}""",
            bodyAfter  = """{"count":42,"label":"changed"}""",
            bodyModify = "count=42&label=changed",
            fieldNames = setOf("count"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testChangedModifiedFieldReturnsTrue_Form() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","age":42}""",
            bodyAfter  = """{"name":"Bob","age":42}""",
            bodyModify = "name=Bob",
            fieldNames = setOf("name"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testOneOfMultipleModifiedFieldsChangedReturnsTrue_Form() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe","email":"a@a.com","age":42}""",
            bodyAfter  = """{"name":"Doe","email":"b@b.com","age":42}""",
            bodyModify = "name=Doe&email=b%40b.com&age=42",
            fieldNames = setOf("name", "email"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testModifiedFieldPresentInBeforeButAbsentInAfterReturnsTrue_Form() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"name":"Doe"}""",
            bodyAfter  = """{"age":42}""",
            bodyModify = "age=42",
            fieldNames = setOf("name"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testChangedIntegerModifiedFieldReturnsTrue_Form() {
        assertTrue(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = """{"count":42,"label":"test"}""",
            bodyAfter  = """{"count":44,"label":"test"}""",
            bodyModify = "count=44&label=test",
            fieldNames = setOf("count"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testInvalidBodiesDifferentReturnsFalse_Form() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid json",
            bodyAfter  = "also not valid json",
            bodyModify = "name=Test",
            fieldNames = setOf("name"),
            bodyParam  = formBodyParam()
        ))
    }

    @Test
    fun testInvalidBodiesSameReturnsFalse_Form() {
        assertFalse(HttpSemanticsOracle.hasChangedModifiedFields(
            bodyBefore = "not valid json",
            bodyAfter  = "not valid json",
            bodyModify = "name=Test",
            fieldNames = setOf("name"),
            bodyParam  = formBodyParam()
        ))
    }
}
