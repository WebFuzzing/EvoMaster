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
    val content: List<McpToolContent> = emptyList(),
    val structuredContent: Map<String, Any?>? = null,
    val isError: Boolean = false
)

/** Result of a `resources/read` invocation, as defined by the MCP specification. */
data class McpResourceResult(
    val contents: List<McpResourceContent> = emptyList()
)

/** Content item within a `resources/read` response (spec: TextResourceContents | BlobResourceContents). */
sealed interface McpResourceContent {
    val uri: String?
    val mimeType: String?
}

data class McpTextResourceContent(
    override val uri: String? = null,
    override val mimeType: String? = null,
    val text: String,
) : McpResourceContent

data class McpBlobResourceContent(
    override val uri: String? = null,
    override val mimeType: String? = null,
    val blob: String,
) : McpResourceContent

/**
 * Content item within a `tools/call` response.
 *
 * Spec union: TextContent | ImageContent | AudioContent | ResourceLink | EmbeddedResource.
 * The [type] discriminator matches the value of the JSON `type` field.
 */
sealed interface McpToolContent {
    val type: String
}

data class McpTextToolContent(
    val text: String,
) : McpToolContent {
    override val type get() = "text"
}

data class McpImageToolContent(
    val data: String,
    val mimeType: String,
) : McpToolContent {
    override val type get() = "image"
}

data class McpAudioToolContent(
    val data: String,
    val mimeType: String,
) : McpToolContent {
    override val type get() = "audio"
}

data class McpResourceLinkToolContent(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null,
) : McpToolContent {
    override val type get() = "resource_link"
}

data class McpEmbeddedResourceToolContent(
    val resource: McpResourceContent,
) : McpToolContent {
    override val type get() = "resource"
}
