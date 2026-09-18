package org.evomaster.core.problem.mcp.builder

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.mcp.McpToolCallAction
import org.evomaster.core.problem.mcp.client.McpToolDefinition
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class McpActionBuilderTest {

    private val mapper = ObjectMapper()
    private val options = RestActionBuilderV3.Options(usingWhiteBox = false)

    private fun tool(name: String, schema: String) =
        McpToolDefinition(name = name, description = "", inputSchema = mapper.readTree(schema))

    private fun buildGene(name: String, schema: String): ObjectGene {
        val messages = mutableListOf<String>()
        return McpActionBuilder.buildInputGene(name, mapper.readTree(schema), options, messages)
    }

    @Test
    fun `builds one action per tool keyed by tool id`() {
        val tools = listOf(
            tool("alpha", """{"type":"object","properties":{"a":{"type":"string"}}}"""),
            tool("beta", """{"type":"object","properties":{"b":{"type":"integer"}}}""")
        )
        val cluster = mutableMapOf<String, McpToolCallAction>()
        val messages = McpActionBuilder.addActionsFromToolList(tools, cluster, options)

        assertEquals(2, cluster.size)
        assertTrue(cluster.containsKey("tool:alpha"))
        assertTrue(cluster.containsKey("tool:beta"))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `object properties become gene fields`() {
        val gene = buildGene(
            "myTool",
            """{"type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer"}},"required":["name"]}"""
        )
        val name = gene.fields.find { it.name == "name" }
        val age = gene.fields.find { it.name == "age" }
        assertNotNull(name)
        assertNotNull(age)
        // required -> not wrapped in OptionalGene; optional -> wrapped
        assertNotNull(name!!.getWrappedGene(StringGene::class.java))
        assertFalse(name is OptionalGene)
        assertTrue(age is OptionalGene)
        assertNotNull(age!!.getWrappedGene(IntegerGene::class.java))
    }

    @Test
    fun `array property becomes ArrayGene`() {
        val gene = buildGene(
            "myTool",
            """{"type":"object","properties":{"tags":{"type":"array","items":{"type":"string"}}}}"""
        )
        val tags = gene.fields.find { it.name == "tags" }
        assertNotNull(tags)
        assertNotNull(tags!!.getWrappedGene(ArrayGene::class.java))
    }

    @Test
    fun `nested defs via ref resolve into nested object gene`() {
        val gene = buildGene(
            "myTool",
            """
            {
              "type":"object",
              "properties": { "child": {"${'$'}ref":"#/${'$'}defs/Child"} },
              "${'$'}defs": { "Child": {"type":"object","properties":{"n":{"type":"integer"}}} }
            }
            """.trimIndent()
        )
        val child = gene.fields.find { it.name == "child" }
        assertNotNull(child)
        val childObj = child!!.getWrappedGene(ObjectGene::class.java)
        assertNotNull(childObj)
        assertNotNull(childObj!!.fields.find { it.name == "n" })
    }

    @Test
    fun `empty inputSchema yields empty object gene`() {
        val gene = buildGene("myTool", """{"type":"object"}""")
        assertTrue(gene.fields.isEmpty())
    }
}
