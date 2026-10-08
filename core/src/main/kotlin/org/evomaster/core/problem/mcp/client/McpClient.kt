package org.evomaster.core.problem.mcp.client

/**
 * Abstraction over the MCP (Model Context Protocol) JSON-RPC transport.
 *
 * Defines the operations available for interaction with an MCP server:
 * capability discovery ([listTools], [listResources], [listResourceTemplates]) and
 * invocation ([callTool], [readResource]).
 *
 */
interface McpClient {
    fun listTools(): List<McpToolDefinition>
    fun listResources(): List<McpResourceDefinition>
    fun listResourceTemplates(): List<McpResourceTemplate>
    fun callTool(name: String, arguments: Map<String, Any?>): McpToolResult
    fun readResource(uri: String): McpResourceResult
}
