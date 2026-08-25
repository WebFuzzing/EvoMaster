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
        val action = McpResourceReadAction(uriTemplate = "http://example.com/resource", uriParams = emptyList())
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
        val action = McpToolCallAction("myTool", inputGene)
        val copy = action.copy() as McpToolCallAction

        assertEquals(action.toolName, copy.toolName)
        assertEquals(action.getName(), copy.getName())
        // Must be independent: different gene instances
        assertNotSame(action.inputSchema, copy.inputSchema)
    }

    @Test
    fun `copy on McpResourceReadAction produces structurally equal but independent copy`() {
        val param = McpUriParam("city", StringGene("city", "paris"))
        val action = McpResourceReadAction(uriTemplate = "weather:///{city}/current", uriParams = listOf(param), isTemplate = true)
        val copy = action.copy() as McpResourceReadAction

        assertEquals(action.isTemplate, copy.isTemplate)
        assertEquals(action.uriTemplate, copy.uriTemplate)
        assertNotSame(action.uriParams[0], copy.uriParams[0])
        assertEquals(action.resolvedUri(), copy.resolvedUri())
    }
}
