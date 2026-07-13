package org.evomaster.core.problem.mcp.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.mcp.McpConst
import org.evomaster.core.remote.HttpClientFactory
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

    private val mapper: ObjectMapper = ObjectMapper()
    private val idCounter = AtomicInteger(1)

    private val client: Client = HttpClientFactory.createTrustingJerseyClient(true, readTimeoutMs)

    @Volatile private var sessionId: String? = null

    private fun nextId() = idCounter.getAndIncrement()

    /** Send a JSON-RPC message. [id] is omitted for notifications. */
    private fun sendJsonRpc(method: String, params: Map<String, Any?>, id: Int?, acceptEventStream: Boolean): Response {
        val payload = mutableMapOf<String, Any?>(
            "jsonrpc" to McpConst.JSONRPC_VERSION,
            "method" to method,
            "params" to params
        )
        id?.let { payload["id"] = it }
        val body = mapper.writeValueAsString(payload)

        val acceptTypes = if (acceptEventStream) arrayOf("application/json", "text/event-stream") else arrayOf("application/json")
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
            "initialize",
            mapOf(
                "protocolVersion" to McpConst.PROTOCOL_VERSION,
                "capabilities" to emptyMap<String, Any>(),
                "clientInfo" to mapOf("name" to "EvoMaster", "version" to "1.0.0")
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
        // Capture session ID before reading the body
        response.getHeaderString(McpConst.SESSION_ID_HEADER)?.let { sessionId = it }
        val responseBody = response.readEntity(String::class.java)
        if (responseBody.isBlank()) {
            throw IllegalStateException("MCP initialize handshake returned empty body")
        }
        // Send the required follow-up notification (fire-and-forget)
        postNotification("notifications/initialized", emptyMap())
    }

    /** Send a JSON-RPC notification (no response expected). */
    private fun postNotification(method: String, params: Map<String, Any?>) {
        val response = sendJsonRpc(method, params, id = null, acceptEventStream = false)
        // Discard the response to complete the HTTP exchange and release the connection
        try { response.close() } catch (_: Exception) {}
    }

    /** Send a JSON-RPC request. Returns null on 4xx (method not supported). */
    private fun post(method: String, params: Map<String, Any?> = emptyMap()): Map<String, Any?>? {
        val response = sendJsonRpc(method, params, nextId(), acceptEventStream = true)
        val status = response.status
        if (status == 400 || status == 404 || status == 405) {
            response.close()
            return null
        }
        val responseBody = response.readEntity(String::class.java)
        return mapper.readValue(responseBody, Map::class.java) as Map<String, Any?>
    }

    override fun listTools(): List<McpToolDefinition> {
        val tools = mutableListOf<McpToolDefinition>()
        var cursor: String? = null
        do {
            val params: Map<String, Any?> = if (cursor != null) mapOf("cursor" to cursor) else emptyMap()
            val response = post("tools/list", params) ?: break
            val result = response["result"] as? Map<String, Any?> ?: break
            val items = result["tools"] as? List<*> ?: emptyList<Any>()
            items.filterIsInstance<Map<String, Any?>>().forEach { t ->
                tools.add(
                    McpToolDefinition(
                        name = t["name"] as String,
                        description = t["description"] as String,
                        inputSchema = mapper.valueToTree(t["inputSchema"])
                    )
                )
            }
            cursor = result["nextCursor"] as? String
        } while (cursor != null)
        return tools
    }

    override fun listResources(): List<McpResourceDefinition> {
        val resources = mutableListOf<McpResourceDefinition>()
        var cursor: String? = null
        do {
            val params: Map<String, Any?> = if (cursor != null) mapOf("cursor" to cursor) else emptyMap()
            val response = post("resources/list", params) ?: break
            val result = response["result"] as? Map<String, Any?> ?: break
            val items = result["resources"] as? List<*> ?: emptyList<Any>()
            items.filterIsInstance<Map<String, Any?>>().forEach { r ->
                resources.add(
                    McpResourceDefinition(
                        uri = r["uri"] as String,
                        name = r["name"] as String,
                        description = r["description"] as? String ?: "",
                        mimeType = r["mimeType"] as? String
                    )
                )
            }
            cursor = result["nextCursor"] as? String
        } while (cursor != null)
        return resources
    }

    override fun listResourceTemplates(): List<McpResourceTemplate> {
        val templates = mutableListOf<McpResourceTemplate>()
        var cursor: String? = null
        do {
            val params: Map<String, Any?> = if (cursor != null) mapOf("cursor" to cursor) else emptyMap()
            val response = post("resources/templates/list", params) ?: break
            val result = response["result"] as? Map<String, Any?> ?: break
            val items = result["resourceTemplates"] as? List<*> ?: emptyList<Any>()
            items.filterIsInstance<Map<String, Any?>>().forEach { t ->
                templates.add(
                    McpResourceTemplate(
                        uriTemplate = t["uriTemplate"] as? String ?: "",
                        name = t["name"] as? String ?: "",
                        description = t["description"] as? String ?: ""
                    )
                )
            }
            cursor = result["nextCursor"] as? String
        } while (cursor != null)
        return templates
    }

    override fun callTool(name: String, arguments: Map<String, Any?>): McpToolResult {
        val response = post("tools/call", mapOf("name" to name, "arguments" to arguments))
            ?: return McpToolResult(isError = true)
        val result = response["result"] as? Map<String, Any?> ?: return McpToolResult(isError = true)
        val rawContent = result["content"] as? List<*> ?: emptyList<Any>()
        val content = rawContent.filterIsInstance<Map<String, Any?>>().map { c ->
            McpContent(
                type = c["type"] as? String ?: "text",
                text = c["text"] as? String,
                uri = c["uri"] as? String,
                mimeType = c["mimeType"] as? String
            )
        }
        return McpToolResult(
            content = content,
            isError = result["isError"] as? Boolean ?: false
        )
    }

    override fun readResource(uri: String): McpResourceResult {
        val response = post("resources/read", mapOf("uri" to uri))
            ?: return McpResourceResult()
        val result = response["result"] as? Map<String, Any?> ?: return McpResourceResult()
        val rawContents = result["contents"] as? List<*> ?: emptyList<Any>()
        val contents = rawContents.filterIsInstance<Map<String, Any?>>().map { c ->
            McpContent(
                type = c["type"] as? String ?: "text",
                text = c["text"] as? String,
                uri = c["uri"] as? String,
                mimeType = c["mimeType"] as? String
            )
        }
        return McpResourceResult(contents = contents)
    }
}
