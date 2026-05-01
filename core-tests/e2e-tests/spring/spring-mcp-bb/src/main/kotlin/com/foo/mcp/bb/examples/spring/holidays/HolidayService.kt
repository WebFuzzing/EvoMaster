package com.foo.mcp.bb.examples.spring.holidays

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.util.*

@Service
class HolidayService {

    private val destinations: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    init {
        setDestinations()
    }

    fun setDestinations() {
        destinations["bali"] = destination(
            "Bali, Indonesia",
            "A tropical paradise of Hindu temples, lush rice terraces, volcanic peaks, and white-sand beaches.",
        )
        destinations["paris"] = destination(
            "Paris, France",
            "The City of Light: world-class art, haute cuisine, grand boulevards, and the iconic Eiffel Tower.",
        )
        destinations["tokyo"] = destination(
            "Tokyo, Japan",
            "A dazzling blend of ultra-modern skyscrapers, ancient shrines, anime culture, and Michelin-starred dining.",
        )
        destinations["new_york"] = destination(
            "New York City, USA",
            "The city that never sleeps: Broadway shows, world-famous museums, Central Park, and a skyline unlike any other.",
        )
    }

    private fun destination(name: String, description: String): MutableMap<String, String> {
        val result: MutableMap<String, String> = LinkedHashMap<String, String>()
        result["name"] = name
        result["description"] = description
        return result
    }

    @Tool(name = "list_destinations", description = "List all available holiday destinations with a short description.")
    fun listDestinations(): MutableMap<String, Any> {
        val list: MutableList<MutableMap<String, Any>> = mutableListOf()
        for (entry in destinations.entries) {
            val item: MutableMap<String, Any> = LinkedHashMap()
            item["id"] = entry.key
            item["name"] = entry.value["name"] as Any
            item["description"] = entry.value["description"] as Any
            list.add(item)
        }
        val text = formatDestinationList(list)
        return textResult(text)
    }

    @Tool(
        name = "get_destination_info",
        description = "Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."
    )
    fun getDestinationInfo(
        @ToolParam(description = "Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs.")
        destinationId: String
    ): MutableMap<String, Any> {
        if (destinationId.isBlank()) {
            return textResult("Error: 'destination' parameter is required.", true)
        }
        val destination = destinations[destinationId.lowercase(Locale.getDefault()).replace(" ", "_")]
        if (destination == null) {
            return textResult(
                "Destination '$destinationId' not found. Call list_destinations to see available options.",
                true
            )
        }

        return textResult(destination["description"]!!)
    }

    private fun textResult(text: String): MutableMap<String, Any> {
        return textResult(text, false)
    }

    private fun textResult(text: String, isError: Boolean): MutableMap<String, Any> {
        val content: MutableMap<String, Any> = LinkedHashMap()
        content["type"] = "text"
        content["text"] = text
        val result: MutableMap<String, Any> = LinkedHashMap()
        result["content"] = mutableListOf(content)
        if (isError) result["isError"] = true
        return result
    }

    private fun formatDestinationList(list: MutableList<MutableMap<String, Any>>): String {
        val sb = StringBuilder("# Available Holiday Destinations\n\n")
        for (item in list) {
            sb.append("- **${item["id"]}** — ${item["name"]}: ${item["description"]}\n")
        }
        sb.append("\nUse `get_destination_info` with one of these IDs for full details.")
        return sb.toString()
    }

}
