package org.evomaster.core.problem.mcp.client

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class HttpMcpClient(private val baseUrl: String) : McpClient {

    private val mapper: ObjectMapper = ObjectMapper()
    private val idCounter = AtomicInteger(1)

    private fun nextId() = idCounter.getAndIncrement()

    private fun openConnection(method: String, params: Map<String, Any?>): Pair<HttpURLConnection, String> {
        val body = mapper.writeValueAsString(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to method,
                "params" to params,
                "id" to nextId()
            )
        )
        val conn = URL(baseUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json, text/event-stream")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return Pair(conn, body)
    }

    /** Send a JSON-RPC request. Throws on connection errors; throws on 5xx. Returns null on 4xx (method not supported). */
    @Suppress("UNCHECKED_CAST")
    private fun post(method: String, params: Map<String, Any?> = emptyMap()): Map<String, Any?>? {
        val (conn, _) = openConnection(method, params)
        val status = conn.responseCode
        if (status == 400 || status == 404 || status == 405) {
            // server does not support this MCP method — treat as empty
            return null
        }
        val responseBody = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        return mapper.readValue(responseBody, Map::class.java) as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
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
                        name = t["name"] as? String ?: "",
                        description = t["description"] as? String ?: "",
                        inputSchema = t["inputSchema"] as? Map<String, Any?> ?: emptyMap()
                    )
                )
            }
            cursor = result["nextCursor"] as? String
        } while (cursor != null)
        return tools
    }

    @Suppress("UNCHECKED_CAST")
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
                        uri = r["uri"] as? String ?: "",
                        name = r["name"] as? String ?: "",
                        description = r["description"] as? String ?: "",
                        mimeType = r["mimeType"] as? String
                    )
                )
            }
            cursor = result["nextCursor"] as? String
        } while (cursor != null)
        return resources
    }

    @Suppress("UNCHECKED_CAST")
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

    @Suppress("UNCHECKED_CAST")
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

    @Suppress("UNCHECKED_CAST")
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
