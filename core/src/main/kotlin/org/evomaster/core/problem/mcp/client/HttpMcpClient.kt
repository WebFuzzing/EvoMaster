package org.evomaster.core.problem.mcp.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.mcp.McpConst
import org.evomaster.core.remote.HttpClientFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * [McpClient] implementation that uses the Streamable HTTP transport.
 *
 * All MCP messages are sent as HTTP POST requests to [baseUrl] using JSON-RPC 2.0, via the
 * Jersey client from [HttpClientFactory].
 *
 * **Session lifecycle**: [initialize] must be invoked once before any other method. It performs
 * the two-step MCP handshake (`initialize` request + `notifications/initialized` notification)
 * and captures the `Mcp-Session-Id` header returned by the server.
 *
 * @param baseUrl the full URL of the MCP endpoint.
 * @param readTimeoutMs read timeout (in milliseconds) for the underlying Jersey client.
 */
class HttpMcpClient(private val baseUrl: String, readTimeoutMs: Int = 60_000) : McpClient {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpMcpClient::class.java)
    }

    private val mapper: ObjectMapper = ObjectMapper()
    private val idCounter = AtomicInteger(1)

    private val client: Client = HttpClientFactory.createTrustingJerseyClient(true, readTimeoutMs)

    // sessionId is volatile to make writes visible across threads
    // TO-DO: Implement thread-safe reconnect on session expiry
    @Volatile private var sessionId: String? = null

    private fun nextId() = idCounter.getAndIncrement()

    /** EvoMaster's version */
    private fun emVersion(): String =
        this::class.java.`package`?.implementationVersion ?: "SNAPSHOT"

    /** Send a JSON-RPC message. [id] is omitted for notifications. */
    private fun sendJsonRpc(method: String, params: Map<String, Any?>, id: Int?, acceptEventStream: Boolean): Response {
        val payload = mutableMapOf<String, Any?>(
            "jsonrpc" to McpConst.JSONRPC_VERSION,
            "method" to method,
            "params" to params
        )
        id?.let { payload["id"] = it }
        val body = mapper.writeValueAsString(payload)

        val acceptTypes = if (acceptEventStream) {
            arrayOf(MediaType.APPLICATION_JSON, MediaType.SERVER_SENT_EVENTS)
        } else {
            arrayOf(MediaType.APPLICATION_JSON)
        }
        
        var builder = client.target(baseUrl).request(*acceptTypes)
        sessionId?.let { builder = builder.header(McpConst.SESSION_ID_HEADER, it) }

        return builder.buildPost(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)).invoke()
    }

    /**
     * Perform the MCP initialization handshake (initialize + notifications/initialized).
     * Must be called once before any other method.
     */
    fun initialize() {
        val response = sendJsonRpc(
            McpConst.METHOD_INITIALIZE,
            mapOf(
                "protocolVersion" to McpConst.PROTOCOL_VERSION,
                "capabilities" to emptyMap<String, Any>(),
                "clientInfo" to mapOf("name" to "EvoMaster", "version" to emVersion())
            ),
            nextId(),
            acceptEventStream = true
        )
        val status = response.status
        if (status >= 400) {
            throw IllegalStateException(
                "MCP initialize handshake failed with HTTP $status at '$baseUrl'"
            )
        }
        // Capture session ID before reading the body.
        // The session id is optional it is missing for example in stateless MCP servers.
        // Reference: https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#session-management
        response.getHeaderString(McpConst.SESSION_ID_HEADER)?.let { sessionId = it }
        val responseBody = response.readEntity(String::class.java)
        if (responseBody.isBlank()) {
            throw IllegalStateException("MCP initialize handshake returned empty body")
        }
        // Send the required follow-up notification (fire-and-forget)
        postNotification(McpConst.INITIALIZED_NOTIFICATION, emptyMap())
    }

    /** Send a JSON-RPC notification (no response expected). */
    private fun postNotification(method: String, params: Map<String, Any?>) {
        val response = sendJsonRpc(method, params, id = null, acceptEventStream = false)
        // Discard the response to complete the HTTP exchange and release the connection
        try {
            response.close()
        } catch (e: Exception) {
            log.warn("Failed to close MCP notification response for '$method'", e)
        }
    }

    /** Send a JSON-RPC request and parse the response. */
    private fun post(method: String, params: Map<String, Any?> = emptyMap()): Map<String, Any?>? {
        val response = sendJsonRpc(method, params, nextId(), acceptEventStream = true)
        val responseBody = response.readEntity(String::class.java)
        return mapper.readValue(responseBody, Map::class.java) as Map<String, Any?>
    }

    /**
     * Fetches all pages from an MCP server response
     *
     * @param method the MCP method name
     * @param resultKey the key in the result map containing the list of items
     * @param transform function to convert each raw response to the target type
     * @return list of all items
     */
    private fun <T> fetchPaginatedList(
        method: String,
        resultKey: String,
        transform: (Map<String, Any?>) -> T
    ): List<T> {
        val results = mutableListOf<T>()
        var cursor: String? = null

        do {
            // On first request cursor is null.
            // On subsequent requests, include the cursor returned by the previous response.
            val params = if (cursor != null) mapOf("cursor" to cursor) else emptyMap()

            val response = post(method, params)
                ?: throw IllegalStateException("$method request failed: received null response")

            val result = response["result"] as? Map<String, Any?>
                ?: throw IllegalStateException("$method response missing 'result' field or invalid type")

            val items = result[resultKey] as? List<Map<String, Any?>> ?: emptyList()
            items.forEach { item ->
                results.add(transform(item))
            }

            // Extract cursor for next page
            cursor = result["nextCursor"] as? String
        } while (cursor != null)

        return results
    }

    private fun getToolResponseContent(name: String, mcpResponse: Map<String, Any?>?) : List<McpToolContent> {
        val content = mcpResponse?.get("content") as? List<Map<String, Any?>> ?: emptyList()
        return content.map { item ->
            val type = item["type"] as? String
                ?: throw IllegalStateException("Tool: $name response content item is missing required 'type' field")
            when (type) {
                McpConst.CONTENT_TYPE_TEXT -> McpTextToolContent(item["text"] as? String ?: "")
                McpConst.CONTENT_TYPE_IMAGE -> McpImageToolContent(
                    data = item["data"] as? String ?: "",
                    mimeType = item["mimeType"] as? String ?: ""
                )
                McpConst.CONTENT_TYPE_AUDIO -> McpAudioToolContent(
                    data = item["data"] as? String ?: "",
                    mimeType = item["mimeType"] as? String ?: ""
                )
                McpConst.CONTENT_TYPE_RESOURCE_LINK -> McpResourceLinkToolContent(
                    uri = item["uri"] as? String ?: "",
                    name = item["name"] as? String ?: "",
                    description = item["description"] as? String,
                    mimeType = item["mimeType"] as? String
                )
                McpConst.CONTENT_TYPE_RESOURCE -> McpEmbeddedResourceToolContent(
                    parseResourceContent(item["resource"] as? Map<String, Any?> ?: emptyMap())
                )
                else -> throw IllegalStateException("Tool: $name response has unsupported content type '$type'")
            }
        }
    }

    private fun getResourceResponseContent(mcpResponse: Map<String, Any?>?) : List<McpResourceContent> {
        val contents = mcpResponse?.get("contents") as? List<Map<String, Any?>> ?: emptyList()
        return contents.map { parseResourceContent(it) }
    }

    /** Parse a single resource-content object into its text/blob variant. */
    private fun parseResourceContent(item: Map<String, Any?>): McpResourceContent {
        val uri = item["uri"] as? String
        val mimeType = item["mimeType"] as? String
        return when {
            item["blob"] != null -> McpBlobResourceContent(uri, mimeType, item["blob"] as String)
            else -> McpTextResourceContent(uri, mimeType, item["text"] as? String ?: "")
        }
    }

    override fun listTools(): List<McpToolDefinition> {
        return fetchPaginatedList(McpConst.METHOD_TOOLS_LIST, "tools", { tool ->
            McpToolDefinition(
                name = tool["name"] as String,
                description = tool["description"] as String,
                inputSchema = mapper.valueToTree(tool["inputSchema"])
            )
        })
    }

    override fun listResources(): List<McpResourceDefinition> {
        return fetchPaginatedList(McpConst.METHOD_RESOURCES_LIST, "resources", { resource ->
            McpResourceDefinition(
                uri = resource["uri"] as String,
                name = resource["name"] as String,
                description = resource["description"] as? String ?: "",
                mimeType = resource["mimeType"] as? String
            )
        })
    }

    override fun listResourceTemplates(): List<McpResourceTemplate> {
        return fetchPaginatedList(McpConst.METHOD_RESOURCE_TEMPLATES_LIST, "resourceTemplates", { template ->
            McpResourceTemplate(
                uriTemplate = template["uriTemplate"] as String,
                name = template["name"] as String,
                description = template["description"] as? String ?: ""
            )
        })
    }

    override fun callTool(name: String, arguments: Map<String, Any?>): McpToolResult {
        val response = post(McpConst.METHOD_TOOLS_CALL, mapOf("name" to name, "arguments" to arguments))
            ?: return McpToolResult(isError = true)
        val result = response["result"] as? Map<String, Any?> ?: return McpToolResult(isError = true)
        val content = getToolResponseContent(name, result)
        val structuredContent = result["structuredContent"] as? Map<String, Any?>
        return McpToolResult(
            content = content,
            structuredContent = structuredContent,
            isError = result["isError"] as? Boolean ?: false
        )
    }

    override fun readResource(uri: String): McpResourceResult {
        val response = post(McpConst.METHOD_RESOURCES_READ, mapOf("uri" to uri))
            ?: return McpResourceResult()
        val result = response["result"] as? Map<String, Any?> ?: return McpResourceResult()
        val contents = getResourceResponseContent(result)
        return McpResourceResult(contents = contents)
    }
}
