package org.evomaster.core.problem.mcp

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class McpActionTest {

    @Test
    fun `McpToolCallAction getName returns tool colon toolName`() {
        val inputGene = ObjectGene("input", emptyList())
        val action = McpToolCallAction("myTool", inputGene)
        assertEquals("tool:myTool", action.getName())
    }

    @Test
    fun `McpResourceReadAction getName starts with resource colon`() {
        val uriGene = StringGene("uri", "http://example.com/resource")
        val action = McpResourceReadAction(uriGene)
        assertTrue(action.getName().startsWith("resource:"))
    }

    @Test
    fun `seeTopGenes on McpToolCallAction returns the ObjectGene`() {
        val inputGene = ObjectGene("input", emptyList())
        val action = McpToolCallAction("myTool", inputGene)
        val topGenes = action.seeTopGenes()
        assertEquals(1, topGenes.size)
        assertSame(inputGene, topGenes[0])
    }

    @Test
    fun `copy on McpToolCallAction produces structurally equal but independent copy`() {
        val inputGene = ObjectGene("input", emptyList())
        val action = McpToolCallAction("myTool", inputGene, "a tool description")
        val copy = action.copy() as McpToolCallAction

        assertEquals(action.toolName, copy.toolName)
        assertEquals(action.description, copy.description)
        assertEquals(action.getName(), copy.getName())
        // Must be independent: different gene instances
        assertNotSame(action.inputSchema, copy.inputSchema)
    }

    @Test
    fun `copy on McpResourceReadAction produces structurally equal but independent copy`() {
        val uriGene = StringGene("uri", "file:///data/res")
        val action = McpResourceReadAction(uriGene, isTemplate = true)
        val copy = action.copy() as McpResourceReadAction

        assertEquals(action.isTemplate, copy.isTemplate)
        assertNotSame(action.uri, copy.uri)
        assertEquals(action.uri.value, copy.uri.value)
    }
}
