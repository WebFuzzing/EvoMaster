package org.evomaster.core.problem.mcp.client

class HttpMcpClient(private val baseUrl: String) : McpClient {

    fun initialize() {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun listTools(): List<McpToolDefinition> {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun listResources(): List<McpResourceDefinition> {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun listResourceTemplates(): List<McpResourceTemplate> {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun callTool(name: String, arguments: Map<String, Any?>): McpToolResult {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun readResource(uri: String): McpResourceResult {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }
}
