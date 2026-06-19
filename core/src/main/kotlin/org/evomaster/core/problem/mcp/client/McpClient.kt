package org.evomaster.core.problem.mcp.client

interface McpClient {
    fun listTools(): List<McpToolDefinition>
    fun listResources(): List<McpResourceDefinition>
    fun listResourceTemplates(): List<McpResourceTemplate>
    fun callTool(name: String, arguments: Map<String, Any?>): McpToolResult
    fun readResource(uri: String): McpResourceResult
}
