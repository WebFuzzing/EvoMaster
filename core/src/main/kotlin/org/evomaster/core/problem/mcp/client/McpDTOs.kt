package org.evomaster.core.problem.mcp.client

import com.fasterxml.jackson.databind.JsonNode

/** Tool definition as returned by the MCP `tools/list` response. */
data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonNode
)

/** Static resource as returned by the MCP `resources/list` response. */
data class McpResourceDefinition(
    val uri: String,
    val name: String,
    val description: String = "",
    val mimeType: String? = null
)

/** URI-template resource as returned by the MCP `resources/templates/list` response. */
data class McpResourceTemplate(
    val uriTemplate: String,
    val name: String,
    val description: String = ""
)

/** Result of a `tools/call` invocation, as defined by the MCP specification. */
data class McpToolResult(
    val content: List<McpContent> = emptyList(),
    val isError: Boolean = false
)

/** Result of a `resources/read` invocation, as defined by the MCP specification. */
data class McpResourceResult(
    val contents: List<McpContent> = emptyList()
)

/** Content item within a tool or resource response. */
data class McpContent(
    val type: String,
    val text: String? = null,
    val uri: String? = null,
    val mimeType: String? = null
)
