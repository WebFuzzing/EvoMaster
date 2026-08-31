package org.evomaster.core.problem.mcp.builder

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonSchemaToOpenApiConverterTest {

    private val mapper = ObjectMapper()

    private fun convert(json: String, root: String = "root"): Pair<Map<String, com.fasterxml.jackson.databind.JsonNode>, MutableList<String>> {
        val messages = mutableListOf<String>()
        val schemas = JsonSchemaToOpenApiConverter.convert(root, mapper.readTree(json), messages)
        return schemas to messages
    }

    @Test
    fun `simple object schema is kept and keyed by root name`() {
        val (schemas, messages) = convert(
            """{ "type":"object", "properties": { "a": {"type":"string"} }, "required":["a"] }"""
        )
        assertTrue(schemas.containsKey("root"))
        assertEquals(1, schemas.size)
        assertEquals("object", schemas["root"]!!.get("type").asText())
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `nullable type array is preserved for the 3_1 parser`() {
        val (schemas, messages) = convert(
            """{ "type":"object", "properties": { "a": {"type":["string","null"]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        // OpenAPI 3.1 consumes JSON Schema type arrays directly: no downgrade to type+nullable.
        assertTrue(a.get("type").isArray)
        assertEquals(setOf("string", "null"), a.get("type").map { it.asText() }.toSet())
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `multiple non-null types are preserved as a type array`() {
        val (schemas, messages) = convert(
            """{ "type":"object", "properties": { "a": {"type":["string","integer"]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertTrue(a.get("type").isArray)
        assertEquals(setOf("string", "integer"), a.get("type").map { it.asText() }.toSet())
        assertFalse(a.has("oneOf"))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `const becomes single-value enum`() {
        val (schemas, _) = convert(
            """{ "type":"object", "properties": { "a": {"type":"string","const":"fixed"} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertNull(a.get("const"))
        assertEquals(1, a.get("enum").size())
        assertEquals("fixed", a.get("enum").get(0).asText())
    }

    @Test
    fun `examples array is preserved`() {
        val (schemas, messages) = convert(
            """{ "type":"object", "properties": { "a": {"type":"string","examples":["x","y"]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        // 3.1 keeps the JSON Schema `examples` array; no collapse to a single `example`.
        assertTrue(a.get("examples").isArray)
        assertEquals(2, a.get("examples").size())
        assertNull(a.get("example"))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `numeric exclusive bounds are preserved`() {
        val (schemas, messages) = convert(
            """{ "type":"object", "properties": { "a": {"type":"integer","exclusiveMinimum":0,"exclusiveMaximum":10} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        // 3.1 uses the numeric JSON Schema form directly; the gene builder reads exclusive*Value.
        assertEquals(0, a.get("exclusiveMinimum").asInt())
        assertEquals(10, a.get("exclusiveMaximum").asInt())
        assertNull(a.get("minimum"))
        assertNull(a.get("maximum"))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `defs are lifted to siblings and refs rewritten`() {
        val (schemas, _) = convert(
            """
            {
              "type":"object",
              "properties": { "child": {"${'$'}ref":"#/${'$'}defs/Child"} },
              "${'$'}defs": { "Child": {"type":"object","properties":{"n":{"type":"integer"}}} }
            }
            """.trimIndent()
        )
        assertTrue(schemas.containsKey("Child"))
        val child = schemas["root"]!!.get("properties").get("child")
        assertEquals("#/components/schemas/Child", child.get("\$ref").asText())
        // def content preserved and no leftover $defs on the root
        assertNull(schemas["root"]!!.get("\$defs"))
        assertEquals("object", schemas["Child"]!!.get("type").asText())
    }

    @Test
    fun `def name colliding with root is disambiguated`() {
        val (schemas, _) = convert(
            """
            {
              "type":"object",
              "properties": { "self": {"${'$'}ref":"#/${'$'}defs/root"} },
              "${'$'}defs": { "root": {"type":"object"} }
            }
            """.trimIndent(),
            root = "root"
        )
        assertTrue(schemas.containsKey("root_def"))
        val self = schemas["root"]!!.get("properties").get("self")
        assertEquals("#/components/schemas/root_def", self.get("\$ref").asText())
    }

    @Test
    fun `conditional keywords are preserved for the 3_1 parser`() {
        val (schemas, messages) = convert(
            """
            {
              "type":"object",
              "properties": { "a": {"type":"string"} },
              "patternProperties": { "^x": {"type":"string"} },
              "if": {"type":"object"}, "then": {"type":"object"}
            }
            """.trimIndent()
        )
        val root = schemas["root"]!!
        // 3.1 accepts these keywords; the gene builder ignores what it does not model, no warnings.
        assertTrue(root.has("patternProperties"))
        assertTrue(root.has("if"))
        assertTrue(root.has("then"))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `prefixItems collapse to items with a warning`() {
        val (schemas, messages) = convert(
            """{ "type":"object", "properties": { "a": {"type":"array","prefixItems":[{"type":"string"},{"type":"integer"}]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertNull(a.get("prefixItems"))
        assertTrue(a.has("items"))
        assertTrue(a.get("items").has("oneOf"))
        assertTrue(messages.any { it.contains("prefixItems") })
    }

    @Test
    fun `missing root type defaults to object`() {
        val (schemas, _) = convert("""{ "properties": { "a": {"type":"string"} } }""")
        assertEquals("object", schemas["root"]!!.get("type").asText())
    }

    @Test
    fun `non-object root is replaced with empty object and warns`() {
        val (schemas, messages) = convert("""{ "type":"string" }""")
        assertTrue(messages.any { it.contains("expected to be an object") })
        assertEquals("object", schemas["root"]!!.get("type").asText())
    }

    @Test
    fun `caller node is not mutated`() {
        val original = """{ "type":"object", "properties": { "a": {"type":["string","null"]} } }"""
        val node = mapper.readTree(original)
        JsonSchemaToOpenApiConverter.convert("root", node, mutableListOf())
        // the original node still has the array type untouched
        assertTrue(node.get("properties").get("a").get("type").isArray)
    }
}
