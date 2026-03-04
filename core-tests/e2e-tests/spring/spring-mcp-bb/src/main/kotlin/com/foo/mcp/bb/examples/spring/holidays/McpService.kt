package com.foo.mcp.bb.examples.spring.holidays

import java.lang.Boolean
import java.util.Map
import kotlin.Any
import kotlin.Int
import kotlin.Number
import kotlin.String

class McpService {

    val PROTOCOL_VERSION: String = "2024-11-05"

    private var holidayService: HolidayService? = null

    init {
        this.holidayService = HolidayService()
    }

    /**
     * Dispatch a JSON-RPC request and return the response map, or `null`
     * for notifications (no response expected).
     */
    fun handle(request: MutableMap<String, Any>): MutableMap<String?, Any?>? {
        val method = request["method"] as String?
        val id = request["id"]

        // Notifications have no "id" — process but do not reply
        val isNotification = !request.containsKey("id")

        val params = request["params"] as? MutableMap<*, *> ?: Map.of<String?, Any?>()

        if (method == null) {
            if (isNotification) return null
            return errorResponse(id, -32600, "Invalid Request: missing method")
        }

        val result = when (method) {
            "initialize" -> handleInitialize(params)
            "ping" -> Map.of<String?, Any?>()
            "tools/list" -> handleToolsList()
            "tools/call" -> handleToolsCall(params)
            "notifications/initialized", "notifications/cancelled", "notifications/progress" -> null
            else -> if (isNotification)
                null
            else
                errorResponse(id, -32601, "Method not found: $method")
        }

        // Notification or a method that explicitly returns null → no response
        if (result == null) return null

        // Build JSON-RPC success response
        val response: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        response["jsonrpc"] = "2.0"
        response["id"] = id

        // If result is already an error envelope (has "code" key) keep as-is,
        // otherwise wrap in "result"
        if (result.containsKey("code")) {
            response["error"] = result
        } else {
            response["result"] = result
        }
        return response
    }


    // ─── Method handlers ──────────────────────────────────────────────────────
    private fun handleInitialize(params: kotlin.collections.Map<out Any?, Any?>?): MutableMap<String?, Any?> {
        val serverInfo: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        serverInfo["name"] = "holiday-mcp-server"
        serverInfo["version"] = "1.0.0"

        val capabilities: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        capabilities["tools"] = Map.of<Any?, Any?>()

        val result: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        result["protocolVersion"] = PROTOCOL_VERSION
        result["capabilities"] = capabilities
        result["serverInfo"] = serverInfo
        return result
    }

    private fun handleToolsList(): MutableMap<String?, Any?> {
        val tools: MutableList<MutableMap<String?, Any?>?> = ArrayList<MutableMap<String?, Any?>?>()

        tools.add(
            tool(
                "list_destinations",
                "List all available holiday destinations with a short description.",
                schema()
            )
        )

        tools.add(
            tool(
                "get_destination_info",
                "Get full details about a holiday destination: highlights, best travel months, weather, currency, and language.",
                schema(
                    prop(
                        "destination",
                        "string",
                        "Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs.",
                        true
                    )
                )
            )
        )

        tools.add(
            tool(
                "get_packages",
                "Get holiday packages for a destination. Optionally filter by package type.",
                schema(
                    prop("destination", "string", "Destination ID. Use list_destinations to see all IDs.", true),
                    prop("type", "string", "Package tier: budget, standard, or luxury. Omit for all tiers.", false)
                )
            )
        )

        tools.add(
            tool(
                "check_availability",
                "Check availability and pricing notes for a destination in a specific month.",
                schema(
                    prop("destination", "string", "Destination ID. Use list_destinations to see all IDs.", true),
                    prop("month", "string", "Month name (e.g. July, December).", true),
                    prop("travelers", "integer", "Number of travellers. Defaults to 2.", false)
                )
            )
        )

        return Map.of<String?, Any?>("tools", tools)
    }

    private fun handleToolsCall(params: kotlin.collections.Map<out Any?, Any?>?): MutableMap<String?, Any?>? {
        val name = params?.get("name") as String?

        val args = params?.get("arguments") as? MutableMap<String, Any?> ?: Map.of<String?, Any?>()

        if (name == null) {
            return toolError("Missing 'name' in tools/call params.")
        }

        return when (name) {
            "list_destinations" -> holidayService!!.listDestinations()
            "get_destination_info" -> holidayService!!.getDestinationInfo(str(args, "destination"))
            "get_packages" -> holidayService!!.getPackages(str(args, "destination"), str(args, "type"))
            "check_availability" -> holidayService!!.checkAvailability(
                str(args, "destination"),
                str(args, "month"),
                intVal(args, "travelers")
            )

            else -> toolError("Unknown tool: $name")
        }
    }


    // ─── Schema / tool builders ───────────────────────────────────────────────
    private fun tool(
        name: String?,
        description: String?,
        inputSchema: MutableMap<String?, Any?>?
    ): MutableMap<String?, Any?> {
        val t: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        t["name"] = name
        t["description"] = description
        t["inputSchema"] = inputSchema
        return t
    }

    /** Build an input schema with zero or more properties.  */
    @SafeVarargs
    private fun schema(vararg properties: MutableMap<String?, Any?>): MutableMap<String?, Any?> {
        val props: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        val required: MutableList<String?> = ArrayList<String?>()

        for (p in properties) {
            val propName = p["_name"] as String?
            val isRequired = Boolean.TRUE == p["_required"]
            p.remove("_name")
            p.remove("_required")
            props[propName] = p
            if (isRequired) required.add(propName)
        }

        val s: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        s["type"] = "object"
        s["properties"] = props
        if (!required.isEmpty()) s["required"] = required
        return s
    }

    private fun prop(
        name: String?,
        type: String?,
        description: String?,
        required: kotlin.Boolean
    ): MutableMap<String?, Any?> {
        val p: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        p["_name"] = name
        p["_required"] = required
        p["type"] = type
        p["description"] = description
        return p
    }


    // ─── Error helpers ────────────────────────────────────────────────────────
    private fun errorResponse(id: Any?, code: Int, message: String?): MutableMap<String?, Any?> {
        // Returned as the top-level "error" key by handle()
        val error: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        error["code"] = code
        error["message"] = message
        return error
    }

    private fun toolError(message: String?): MutableMap<String?, Any?> {
        val content: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        content["type"] = "text"
        content["text"] = message
        val result: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        result["content"] = listOf<MutableMap<String?, Any?>?>(content)
        result["isError"] = true
        return result
    }


    // ─── Arg helpers ──────────────────────────────────────────────────────────
    private fun str(args: MutableMap<String, Any?>, key: String): String? {
        val v = args[key]
        return v as? String
    }

    private fun intVal(args: MutableMap<String, Any?>, key: String): Int? {
        val v = args[key]
        if (v is Int) return v
        if (v is Number) return v.toInt()
        return null
    }

}
