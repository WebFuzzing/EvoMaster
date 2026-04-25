package org.evomaster.core.problem.rest.oracle

import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
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

    private fun jsonPutBodyParam(
        activeFields: Map<String, String>,
        omittedFields: Set<String> = emptySet()
    ): BodyParam {
        val fields = mutableListOf<org.evomaster.core.search.gene.Gene>()
        activeFields.forEach { (name, value) -> fields.add(StringGene(name, value)) }
        omittedFields.forEach { name ->
            fields.add(OptionalGene(name, StringGene(name, ""), isActive = false))
        }
        val obj = ObjectGene("body", fields = fields)
        val typeGene = EnumGene("contentType", listOf("application/json"))
        typeGene.index = 0
        return BodyParam(obj, typeGene)
    }

    private fun runMismatchedPutOracle(
        path: String,
        putBody: BodyParam?,
        getResponseBody: String,
        schema: RestSchema? = null,
        getResponseStatus: Int = 200,
        putResponseStatus: Int = 200
    ): Boolean {
        val restPath = RestPath(path)
        val put = RestCallAction(
            id = "put", verb = HttpVerb.PUT, path = restPath,
            parameters = if (putBody != null) mutableListOf(putBody) else mutableListOf()
        )
        val get = RestCallAction(
            id = "get", verb = HttpVerb.GET, path = restPath,
            parameters = mutableListOf()
        )

        put.setLocalId("put-action")
        get.setLocalId("get-action")

        val individual = RestIndividual(
            mutableListOf(put, get), SampleType.RANDOM, dbInitialization = mutableListOf()
        )
        individual.doInitialize()

        val putResult = RestCallResult(put.getLocalId()).apply {
            setStatusCode(putResponseStatus)
            setBody("{}")
            setBodyType(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
        }
        val getResult = RestCallResult(get.getLocalId()).apply {
            setStatusCode(getResponseStatus)
            setBody(getResponseBody)
            setBodyType(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
        }

        return HttpSemanticsOracle.hasMismatchedPutResponse(
            individual,
            listOf(putResult, getResult),
            schema
        )
    }

    private fun buildUsersSchema(
        putWritable: List<String>,
        getResponseFields: List<String>
    ): RestSchema {
        fun props(names: List<String>) = names.joinToString(",") {
            "\"$it\":{\"type\":\"string\"}"
        }
        val putSchemaJson  = "{\"type\":\"object\",\"properties\":{${props(putWritable)}}}"
        val getSchemaJson  = "{\"type\":\"object\",\"properties\":{${props(getResponseFields)}}}"

        val json = """
            {
              "openapi": "3.0.0",
              "info": { "title": "test", "version": "1.0" },
              "paths": {
                "/users": {
                  "put": {
                    "requestBody": {
                      "required": true,
                      "content": {
                        "application/json": { "schema": $putSchemaJson }
                      }
                    },
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": { "schema": $getSchemaJson }
                        }
                      }
                    }
                  },
                  "get": {
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": { "schema": $getSchemaJson }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        return RestSchema(OpenApiAccess.parseOpenApi(json, SchemaLocation.MEMORY))
    }

    @Test
    fun testPut_sentFieldsMatch_returnsFalse() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice", "email" to "a@b.c")),
            getResponseBody = """{"name":"Alice","email":"a@b.c"}"""
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_sentFieldHasDifferentValueInGet_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = """{"name":"Bob"}"""
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_sentFieldMissingInGet_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice", "email" to "a@b.c")),
            getResponseBody = """{"name":"Alice"}"""
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_extraFieldInGetResponseIgnored_returnsFalse() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = """{"id":42,"name":"Alice","createdAt":"2026-01-01"}"""
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_wipedFieldStillPresentInGet_returnsTrue() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "email", "role"),
            getResponseFields = listOf("id", "name", "email", "role", "createdAt")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = mapOf("name" to "Alice", "email" to "a@b.c"),
                omittedFields = setOf("role")
            ),
            getResponseBody = """{"id":1,"name":"Alice","email":"a@b.c","role":"admin","createdAt":"2026-01-01"}""",
            schema = schema
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_wipedFieldExplicitlyNullInGet_returnsFalse() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "role"),
            getResponseFields = listOf("name", "role")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = mapOf("name" to "Alice"),
                omittedFields = setOf("role")
            ),
            getResponseBody = """{"name":"Alice","role":null}""",
            schema = schema
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_wipedFieldAbsentInGet_returnsFalse() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "role"),
            getResponseFields = listOf("name", "role")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = mapOf("name" to "Alice"),
                omittedFields = setOf("role")
            ),
            getResponseBody = """{"name":"Alice"}""",
            schema = schema
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_changedField_returnsTrue() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "role"),
            getResponseFields = listOf("id", "name", "role")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = mapOf("name" to "Alice", "role" to "admin"),
            ),
            getResponseBody = """{"id":"1","name":"Alice","role":"user"}""",
            schema = schema
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_changedField_StringNull_returnsFalse() {
        // PUT and GET both carry the literal 4-char string "null" for "name".
        // Sent-fields path: values are equal, so no mismatch is reported.
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "null")),
            getResponseBody = """{"name":"null"}"""
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_wipedField_StringNullInGet_returnsTrue() {
        // PUT omits "role" (full replacement should wipe it).
        // GET returns the literal 4-char string "null" - the field still holds
        // a real value, distinct from JSON null.
        val schema = buildUsersSchema(
            putWritable = listOf("name", "role"),
            getResponseFields = listOf("name", "role")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = mapOf("name" to "Alice"),
                omittedFields = setOf("role")
            ),
            getResponseBody = """{"name":"Alice","role":"null"}""",
            schema = schema
        )
        assertTrue(mismatch)
    }


    @Test
    fun testPut_writeOnlyFieldNotInGetSchema_noFalsePositive() {
        // password is in PUT schema but NOT in GET schema (write-only).
        // It was not sent. The wiped check must NOT flag this as a bug, even
        // though there is no "password" field in the GET response.
        val schema = buildUsersSchema(
            putWritable = listOf("name", "password"),
            getResponseFields = listOf("name")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = mapOf("name" to "Alice"),
                omittedFields = setOf("password")
            ),
            getResponseBody = """{"name":"Alice"}""",
            schema = schema
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_putReturnedNon2xx_returnsFalse() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = """{"name":"Bob"}""",
            putResponseStatus = 400
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_getReturnedNon2xx_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = """{}""",
            getResponseStatus = 404
        )
        assertTrue(mismatch)
    }


    @Test
    fun testPut_allFieldsOmitted_getReturnsOnlyReadOnlySchemaFields_returnsFalse() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "email"),
            getResponseFields = listOf("id", "name", "email", "createdAt")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = emptyMap(),
                omittedFields = setOf("name", "email")
            ),
            getResponseBody = """{"id":42,"createdAt":"2026-01-01"}""",
            schema = schema
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_allFieldsOmitted_getReturnsWritableFieldsAsNull_returnsFalse() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "email"),
            getResponseFields = listOf("id", "name", "email", "createdAt")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(
                activeFields = emptyMap(),
                omittedFields = setOf("name", "email")
            ),
            getResponseBody = """{"id":42,"name":null,"email":null,"createdAt":"2026-01-01"}""",
            schema = schema
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_noBodyParam_getHasServerDefaults_returnsFalse() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = null,
            getResponseBody = """{"id":42,"name":"default","createdAt":"2026-01-01"}"""
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_noBodyParam_getAlsoEmpty_returnsFalse() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = null,
            getResponseBody = ""
        )
        assertFalse(mismatch)
    }

    @Test
    fun testPut_nonEmptyPutBody_getEmptyString_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = ""
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_nonEmptyPutBody_getEmptyJsonObject_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = "{}"
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_nonEmptyPutBody_getLiteralNull_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = "null"
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_nonEmptyPutBody_getGarbageBody_returnsTrue() {
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = jsonPutBodyParam(activeFields = mapOf("name" to "Alice")),
            getResponseBody = "not a valid json body"
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_noBodyParam_schemaProvided_getStillShowsWritableFields_returnsTrue() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "email"),
            getResponseFields = listOf("id", "name", "email", "createdAt")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = null,
            getResponseBody = """{"id":1,"name":"Alice","email":"a@a.com","createdAt":"2026-01-01"}""",
            schema = schema
        )
        assertTrue(mismatch)
    }

    @Test
    fun testPut_noBodyParam_schemaProvided_getHasOnlyReadOnlyFields_returnsFalse() {
        val schema = buildUsersSchema(
            putWritable = listOf("name", "email"),
            getResponseFields = listOf("id", "name", "email", "createdAt")
        )
        val mismatch = runMismatchedPutOracle(
            path = "/users",
            putBody = null,
            getResponseBody = """{"id":42,"createdAt":"2026-01-01"}""",
            schema = schema
        )
        assertFalse(mismatch)
    }
}
