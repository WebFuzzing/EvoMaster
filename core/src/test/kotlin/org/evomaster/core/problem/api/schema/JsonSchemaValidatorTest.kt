package org.evomaster.core.problem.api.schema

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class JsonSchemaValidatorTest {

    private val mapper = ObjectMapper()
    private val validator = JsonSchemaValidator()

    @Test
    fun `validates nested draft 2020-12 schemas with local definitions`() {
        val schema = mapper.readTree(
            """
            {
              "type": "object",
              "properties": { "result": { "${'$'}ref": "#/${'$'}defs/result" } },
              "required": ["result"],
              "${'$'}defs": {
                "result": {
                  "type": "object",
                  "properties": { "name": { "type": "string", "minLength": 2 } },
                  "required": ["name"]
                }
              }
            }
            """.trimIndent()
        )
        val compiled = compiled(validator.compile(schema))

        assertTrue(compiled.validate(mapper.readTree("""{"result":{"name":"EvoMaster"}}""")).isEmpty())

        val violations = compiled.validate(mapper.readTree("""{"result":{"name":"x"}}"""))
        assertTrue(violations.any { it.keyword == "minLength" }, violations.toString())
        assertTrue(violations.all { it.key.startsWith("json-schema:") })
    }

    @Test
    fun `reports stable keys for different invalid values`() {
        val compiled = compiled(
            validator.compile(mapper.readTree("""{"type":"object","properties":{"age":{"type":"integer"}},"required":["age"]}"""))
        )

        val first = compiled.validate(mapper.readTree("""{"age":"young"}""")).singleOrNull { it.keyword == "type" }
            ?: error(compiled.validate(mapper.readTree("""{"age":"young"}""")).toString())
        val second = compiled.validate(mapper.readTree("""{"age":true}""")).singleOrNull { it.keyword == "type" }
            ?: error(compiled.validate(mapper.readTree("""{"age":true}""")).toString())

        assertEquals(first.key, second.key)
        assertTrue(!first.key.contains("young"))
    }

    @Test
    fun `supports type arrays and boolean schemas`() {
        val nullable = compiled(
            validator.compile(mapper.readTree("""{"type":["string","null"]}"""))
        )
        assertTrue(nullable.validate(mapper.readTree("null")).isEmpty())
        val typeViolations = nullable.validate(mapper.readTree("42"))
        assertTrue(typeViolations.any { it.keyword == "unionType" }, typeViolations.toString())

        val falseSchema = compiled(validator.compile(mapper.readTree("false")))
        assertTrue(falseSchema.validate(mapper.readTree("{}")).isNotEmpty())
    }

    @Test
    fun `disallows external schema references by default`() {
        val schema = Files.createTempFile("external-schema", ".json")
        Files.writeString(schema, """{"type":"string"}""")
        val result = validator.compile(mapper.readTree("""{"${'$'}ref":"${schema.toUri()}"}"""))

        val failure = result as? JsonSchemaValidator.CompilationResult.Failure
            ?: error("Expected JSON Schema compilation failure")
        assertEquals("json-schema:invalid-schema:/", failure.issues.single().key)
    }

    @Test
    fun `allows external schema references when enabled`() {
        val schema = Files.createTempFile("external-schema", ".json")
        Files.writeString(schema, """{"type":"string"}""")

        val compiled = compiled(JsonSchemaValidator(allowExternalReferences = true).compile(
            mapper.readTree("""{"${'$'}ref":"${schema.toUri()}"}""")
        ))

        assertTrue(compiled.validate(mapper.readTree("\"valid\" ")).isEmpty())
        assertTrue(compiled.validate(mapper.readTree("42")).isNotEmpty())
    }

    @Test
    fun `reports unresolved local references but allows ref named output fields`() {
        val unresolved = validator.compile(mapper.readTree("""{"${'$'}ref":"#/${'$'}defs/missing"}"""))
        val failure = unresolved as? JsonSchemaValidator.CompilationResult.Failure
            ?: error("Expected unresolved reference failure")
        assertEquals("json-schema:invalid-schema:/", failure.issues.single().key)

        val schema = compiled(validator.compile(mapper.readTree(
            """{"type":"object","properties":{"${'$'}ref":{"type":"string"}}}"""
        )))
        assertTrue(schema.validate(mapper.readTree("""{"${'$'}ref":"value"}""")).isEmpty())
    }

    private fun compiled(result: JsonSchemaValidator.CompilationResult): JsonSchemaValidator.CompiledSchema =
        (result as? JsonSchemaValidator.CompilationResult.Success)?.schema
            ?: error("Expected JSON Schema compilation success: $result")
}
