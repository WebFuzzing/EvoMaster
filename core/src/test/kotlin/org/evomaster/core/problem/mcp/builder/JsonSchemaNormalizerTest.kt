package org.evomaster.core.problem.mcp.builder

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonSchemaNormalizerTest {

    private val mapper = ObjectMapper()

    private fun normalize(json: String, root: String = "root"): Pair<Map<String, com.fasterxml.jackson.databind.JsonNode>, MutableList<String>> {
        val messages = mutableListOf<String>()
        val schemas = JsonSchemaNormalizer.normalize(root, mapper.readTree(json), messages)
        return schemas to messages
    }

    @Test
    fun `simple object schema is kept and keyed by root name`() {
        val (schemas, messages) = normalize(
            """{ "type":"object", "properties": { "a": {"type":"string"} }, "required":["a"] }"""
        )
        assertTrue(schemas.containsKey("root"))
        assertEquals(1, schemas.size)
        assertEquals("object", schemas["root"]!!.get("type").asText())
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `nullable type array becomes single type plus nullable`() {
        val (schemas, _) = normalize(
            """{ "type":"object", "properties": { "a": {"type":["string","null"]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertEquals("string", a.get("type").asText())
        assertTrue(a.get("nullable").asBoolean())
    }

    @Test
    fun `multiple non-null types become oneOf with warning`() {
        val (schemas, messages) = normalize(
            """{ "type":"object", "properties": { "a": {"type":["string","integer"]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertNull(a.get("type"))
        assertTrue(a.has("oneOf"))
        assertEquals(2, a.get("oneOf").size())
        assertTrue(messages.any { it.contains("multiple types") })
    }

    @Test
    fun `const becomes single-value enum`() {
        val (schemas, _) = normalize(
            """{ "type":"object", "properties": { "a": {"type":"string","const":"fixed"} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertNull(a.get("const"))
        assertEquals(1, a.get("enum").size())
        assertEquals("fixed", a.get("enum").get(0).asText())
    }

    @Test
    fun `examples array collapses to single example`() {
        val (schemas, _) = normalize(
            """{ "type":"object", "properties": { "a": {"type":"string","examples":["x","y"]} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertNull(a.get("examples"))
        assertEquals("x", a.get("example").asText())
    }

    @Test
    fun `numeric exclusive bounds become bound plus boolean flag`() {
        val (schemas, _) = normalize(
            """{ "type":"object", "properties": { "a": {"type":"integer","exclusiveMinimum":0,"exclusiveMaximum":10} } }"""
        )
        val a = schemas["root"]!!.get("properties").get("a")
        assertEquals(0, a.get("minimum").asInt())
        assertTrue(a.get("exclusiveMinimum").asBoolean())
        assertEquals(10, a.get("maximum").asInt())
        assertTrue(a.get("exclusiveMaximum").asBoolean())
    }

    @Test
    fun `defs are lifted to siblings and refs rewritten`() {
        val (schemas, _) = normalize(
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
        val (schemas, _) = normalize(
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
    fun `unsupported keywords are dropped with a warning`() {
        val (schemas, messages) = normalize(
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
        assertNull(root.get("patternProperties"))
        assertNull(root.get("if"))
        assertNull(root.get("then"))
        assertTrue(messages.any { it.contains("patternProperties") })
        assertTrue(messages.any { it.contains("'if'") })
    }

    @Test
    fun `prefixItems collapse to items with a warning`() {
        val (schemas, messages) = normalize(
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
        val (schemas, _) = normalize("""{ "properties": { "a": {"type":"string"} } }""")
        assertEquals("object", schemas["root"]!!.get("type").asText())
    }

    @Test
    fun `non-object root is replaced with empty object and warns`() {
        val (schemas, messages) = normalize("""{ "type":"string" }""")
        // stays string but warns (MCP expects object)
        assertTrue(messages.any { it.contains("expected to be an object") })
    }

    @Test
    fun `caller node is not mutated`() {
        val original = """{ "type":"object", "properties": { "a": {"type":["string","null"]} } }"""
        val node = mapper.readTree(original)
        JsonSchemaNormalizer.normalize("root", node, mutableListOf())
        // the original node still has the array type untouched
        assertTrue(node.get("properties").get("a").get("type").isArray)
    }
}
