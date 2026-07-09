package org.evomaster.core.problem.mcp.client

data class McpToolDefinition(
    val name: String,
    val description: String = "",
    val inputSchema: Map<String, Any?> = emptyMap()
)

data class McpResourceDefinition(
    val uri: String,
    val name: String = "",
    val description: String = "",
    val mimeType: String? = null
)

data class McpResourceTemplate(
    val uriTemplate: String,
    val name: String = "",
    val description: String = ""
)

data class McpToolResult(
    val content: List<McpContent> = emptyList(),
    val isError: Boolean = false
)

data class McpResourceResult(
    val contents: List<McpContent> = emptyList()
)

data class McpContent(
    val type: String,
    val text: String? = null,
    val uri: String? = null,
    val mimeType: String? = null
)
