package com.foo.mcp.bb.examples.spring.holidays

import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.mutableMapOf

class McpService {

    val PROTOCOL_VERSION: String = "2024-11-05"

    private var holidayService: HolidayService? = null

    init {
        this.holidayService = HolidayService()
    }

    fun handle(request: MutableMap<String, Any>): MutableMap<String, Any> {
        val method = request["method"] as String?
        val id = request["id"] as Int

        val isNotification = !request.containsKey("id")

        val params = request["params"] as MutableMap<String, Any>

        if (method == null) {
            if (isNotification) return mutableMapOf()
            return errorResponse(-32600, "Invalid Request: missing method")
        }

        val result = when (method) {
            "initialize" -> handleInitialize()
            "ping" -> mutableMapOf()
            "tools/list" -> handleToolsList()
            "tools/call" -> handleToolsCall(params)
            "notifications/initialized", "notifications/cancelled", "notifications/progress" -> null
            else -> if (isNotification)
                null
            else
                errorResponse(-32601, "Method not found: $method")
        }

        if (result == null) return mutableMapOf()

        val response: MutableMap<String, Any> = mutableMapOf()
        response["jsonrpc"] = "2.0"
        response["id"] = id

        if (result.containsKey("code")) {
            response["error"] = result
        } else {
            response["result"] = result
        }
        return response
    }


    private fun handleInitialize(): MutableMap<String, Any> {
        val serverInfo: MutableMap<String, Any> = mutableMapOf()
        serverInfo["name"] = "holiday-mcp-server"
        serverInfo["version"] = "1.0.0"

        val capabilities: MutableMap<String, Any> = mutableMapOf()
        capabilities["tools"] = emptyMap<String, Any>()

        val result: MutableMap<String, Any> = mutableMapOf()
        result["protocolVersion"] = PROTOCOL_VERSION
        result["capabilities"] = capabilities
        result["serverInfo"] = serverInfo
        return result
    }

    private fun handleToolsList(): MutableMap<String, Any> {
        val tools: MutableList<MutableMap<String, Any>> = mutableListOf()
        tools.add(
            tool(
                "list_destinations",
                "List all available holiday destinations with a short description.",
                schema(mutableListOf())
            )
        )
        tools.add(
            tool(
                "get_destination_info",
                "Get full details about a holiday destination: highlights, best travel months, weather, currency, and language.",
                schema(
                    mutableListOf(property(
                        "destination",
                        "string",
                        "Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs.",
                        true
                        )
                    )
                )
            )
        )

        val result = mutableMapOf<String, Any>()
        result["tools"] = tools
        return result
    }

    private fun handleToolsCall(params: Map<String, Any>): MutableMap<String, Any> {
        val name = params["name"] as String?
        val args = params["arguments"] as MutableMap<*, *>

        if (name == null) {
            return toolError("Missing 'name' in tools/call params.")
        }

        return when (name) {
            "list_destinations" -> holidayService!!.listDestinations()
            "get_destination_info" -> holidayService!!.getDestinationInfo(args["destination"] as String)
            else -> toolError("Unknown tool: $name")
        }
    }


    private fun tool(name: String, description: String, inputSchema: MutableMap<String, Any>): MutableMap<String, Any> {
        val result: MutableMap<String, Any> = mutableMapOf()
        result["name"] = name
        result["description"] = description
        result["inputSchema"] = inputSchema
        return result
    }

    private fun schema(properties: MutableList<MutableMap<String, Any>>): MutableMap<String, Any> {
        val props: MutableMap<String, Any> = mutableMapOf()
        val required: MutableList<String> = mutableListOf()

        for (property in properties) {
            val propName = property["_name"] as String
            val isRequired = property["_required"] as Boolean
            property.remove("_name")
            property.remove("_required")
            props[propName] = property
            if (isRequired) required.add(propName)
        }

        val result: MutableMap<String, Any> = mutableMapOf()
        result["type"] = "object"
        result["properties"] = props
        if (!required.isEmpty()) result["required"] = required
        return result
    }

    private fun property(name: String, type: String, description: String, required: Boolean): MutableMap<String, Any> {
        val result: MutableMap<String, Any> = mutableMapOf()
        result["_name"] = name
        result["_required"] = required
        result["type"] = type
        result["description"] = description
        return result
    }

    private fun errorResponse(code: Int, message: String): MutableMap<String, Any> {
        val error: MutableMap<String, Any> = mutableMapOf()
        error["code"] = code
        error["message"] = message
        return error
    }

    private fun toolError(message: String): MutableMap<String, Any> {
        val content: MutableMap<String, Any> = mutableMapOf()
        content["type"] = "text"
        content["text"] = message
        val result: MutableMap<String, Any> = mutableMapOf()
        result["content"] = listOf(content)
        result["isError"] = true
        return result
    }

}
