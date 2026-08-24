package org.evomaster.core.problem.mcp.builder

import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.mcp.McpToolCallAction
import org.evomaster.core.problem.mcp.client.McpToolDefinition
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.gene.ObjectGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Builds [McpToolCallAction]s from the tool definitions discovered on an MCP server.
 *
 * Converts each tool's `inputSchema` (JSON Schema 2020-12 dialect) into an [ObjectGene] that
 * represents the tool's arguments, and registers one action per tool in the action cluster.
 *
 */
object McpActionBuilder {

    private val log: Logger = LoggerFactory.getLogger(McpActionBuilder::class.java)

    /**
     * Build an action per tool and register it in [actionCluster], keyed by the action id.
     *
     * @return warning messages accumulated while building (unsupported schema constructs, skipped tools)
     */
    fun addActionsFromToolList(
        tools: List<McpToolDefinition>,
        actionCluster: MutableMap<String, McpToolCallAction>,
        options: RestActionBuilderV3.Options
    ): List<String> {

        val messages = mutableListOf<String>()

        for (tool in tools) {
            try {
                val gene = buildInputGene(tool.name, tool.inputSchema, options, messages)
                val action = McpToolCallAction(tool.name, gene)
                actionCluster[action.id] = action
            } catch (e: Exception) {
                messages.add("Skipping MCP tool '${tool.name}': ${e.message}")
                log.warn("Failed to build action for MCP tool '{}'", tool.name, e)
            }
        }

        return messages
    }

    /**
     * Convert a single tool's `inputSchema` into an [ObjectGene].
     *
     * @param toolName the MCP tool name, used as the root schema/component name
     * @param inputSchema the raw JSON Schema node from the tool definition
     * @param messages sink for warnings about lossy or unsupported schema constructs
     */
    fun buildInputGene(
        toolName: String,
        inputSchema: JsonNode,
        options: RestActionBuilderV3.Options,
        messages: MutableList<String>
    ): ObjectGene {

        val schemas = JsonSchemaNormalizer.normalize(toolName, inputSchema, messages)
        val gene = RestActionBuilderV3.createGeneForDTOs(toolName, schemas, options)

        return gene as? ObjectGene
            ?: ObjectGene("input", listOf()).also {
                messages.add("MCP tool '$toolName' inputSchema did not resolve to an object gene")
            }
    }
}
