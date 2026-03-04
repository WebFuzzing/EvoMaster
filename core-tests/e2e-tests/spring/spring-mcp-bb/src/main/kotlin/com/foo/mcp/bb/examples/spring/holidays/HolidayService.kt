package com.foo.mcp.bb.examples.spring.holidays

import org.springframework.stereotype.Service
import java.util.*

//@Service
class HolidayService {

    private val destinations: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
    private val packages: MutableMap<String, MutableList<MutableMap<String, Any>>> = mutableMapOf()
    private val highSeasonMonths: MutableSet<String?> = mutableSetOf("June", "July", "August", "December")

    init {
        setDestinations()
        setPackages()
    }

    fun setDestinations() {
        destinations["bali"] = dest(
            "Bali, Indonesia",
            "A tropical paradise of Hindu temples, lush rice terraces, volcanic peaks, and white-sand beaches.",
            mutableListOf("April", "May", "June", "July", "August", "September"),
            mutableListOf(
                "Tanah Lot Temple",
                "Ubud Monkey Forest",
                "Tegallalang Rice Terraces",
                "Seminyak Beach",
                "Mount Batur"
            ),
            "Indonesian Rupiah (IDR)", "Balinese / Indonesian", "28 °C / 82 °F"
        )
        destinations["paris"] = dest(
            "Paris, France",
            "The City of Light: world-class art, haute cuisine, grand boulevards, and the iconic Eiffel Tower.",
            mutableListOf("April", "May", "June", "September", "October"),
            mutableListOf(
                "Eiffel Tower",
                "Louvre Museum",
                "Notre-Dame Cathedral",
                "Champs-Élysées",
                "Versailles Palace"
            ),
            "Euro (EUR)", "French", "15 °C / 59 °F"
        )
        destinations["tokyo"] = dest(
            "Tokyo, Japan",
            "A dazzling blend of ultra-modern skyscrapers, ancient shrines, anime culture, and Michelin-starred dining.",
            mutableListOf("March", "April", "May", "October", "November"),
            mutableListOf(
                "Shinjuku Gyoen",
                "Senso-ji Temple",
                "Akihabara",
                "Tsukiji Fish Market",
                "teamLab Borderless"
            ),
            "Japanese Yen (JPY)", "Japanese", "16 °C / 61 °F"
        )
        destinations["new_york"] = dest(
            "New York City, USA",
            "The city that never sleeps: Broadway shows, world-famous museums, Central Park, and a skyline unlike any other.",
            mutableListOf("April", "May", "June", "September", "October"),
            mutableListOf(
                "Central Park",
                "Times Square",
                "Statue of Liberty",
                "Metropolitan Museum of Art",
                "Brooklyn Bridge"
            ),
            "US Dollar (USD)", "English", "13 °C / 55 °F"
        )
    }

    private fun dest(
        name: String, description: String,
        bestMonths: MutableList<String>, highlights: MutableList<String>,
        currency: String, language: String, avgTemp: String
    ): MutableMap<String, Any> {
        val m: MutableMap<String, Any> = LinkedHashMap<String, Any>()
        m["name"] = name
        m["description"] = description
        m["bestMonths"] = bestMonths
        m["highlights"] = highlights
        m["currency"] = currency
        m["language"] = language
        m["avgTemp"] = avgTemp
        return m
    }

    private fun setPackages() {
        for (destination in destinations) {
            val displayName = destination.value["name"] as String;
            val pkg = mutableListOf(
                pkg(
                    "Budget Escape",
                    displayName,
                    "budget",
                    4,
                    599,
                    "Hostel or 2★ hotel, breakfast included, group tours"
                ),
                pkg(
                    "Standard Getaway",
                    displayName,
                    "standard",
                    7,
                    1499,
                    "4★ hotel, half board, guided city tour + one excursion"
                ),
                pkg(
                    "Luxury Indulgence",
                    displayName,
                    "luxury",
                    10,
                    3999,
                    "5★ resort, all-inclusive, private transfers, spa access"
                )
            )
            packages[destination.key] = pkg
        }
    }

    // ─── Tool implementations ────────────────────────────────────────────────
    /** Returns the list of all destination keys and display names.  */
    fun listDestinations(): MutableMap<String?, Any?> {
        val list: MutableList<MutableMap<String?, Any?>> = ArrayList<MutableMap<String?, Any?>>()
        for (entry in destinations.entries) {
            val item: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
            item["id"] = entry.key
            item["name"] = entry.value["name"]
            item["description"] = entry.value["description"]
            list.add(item)
        }
        val text = formatDestinationList(list)
        return textResult(text)
    }

    /** Returns detailed info about a single destination.  */
    fun getDestinationInfo(destinationId: String?): MutableMap<String?, Any?> {
        if (destinationId.isNullOrBlank()) {
            return textResult("Error: 'destination' parameter is required.", true)
        }
        val d = destinations[destinationId.lowercase(Locale.getDefault()).replace(" ", "_")]
        if (d == null) {
            return textResult(
                "Destination '$destinationId' not found. Call list_destinations to see available options.",
                true
            )
        }

        val sb = StringBuilder()
        sb.append("# ${d["name"]}\n\n")
        .append(d["description"]).append("\n\n")
        .append("**Best months to visit:** ").append(java.lang.String.join(", ", castList(d["bestMonths"])))
            .append("\n")
        .append("**Average temperature:** ${d["avgTemp"]}\n")
        .append("**Currency:** ${d["currency"]}\n")
        .append("**Language(s):** ${d["language"]}\n\n")
        .append("**Top highlights:**\n")
        for (h in castList(d["highlights"])!!) {
            sb.append("  - $h\n")
        }
        return textResult(sb.toString())
    }

    /** Returns holiday packages for a destination, optionally filtered by type.  */
    fun getPackages(destinationId: String?, type: String?): MutableMap<String?, Any?> {
        if (destinationId.isNullOrBlank()) {
            return textResult("Error: 'destination' parameter is required.", true)
        }
        val key = destinationId.lowercase(Locale.getDefault()).replace(" ", "_")
        val pkgs = packages[key] ?: return textResult(
            "No packages found for '$destinationId'. Call list_destinations to see available options.",
            true
        )

        var filtered: MutableList<MutableMap<String, Any>> = pkgs
        if (!type.isNullOrBlank()) {
            val t = type.lowercase(Locale.getDefault())
            filtered = pkgs.stream().filter { p: MutableMap<String, Any> -> t == p["type"] }.toList()
            if (filtered.isEmpty()) {
                return textResult(
                    "No '$type' packages found for $destinationId. Valid types: budget, standard, luxury.",
                    true
                )
            }
        }

        val sb = StringBuilder()
        val destName = destinations[key]!!["name"] as String?
        sb.append("# Holiday Packages — $destName\n\n")
        for (p in filtered) {
            sb.append("## ${p["name"]} (${p["type"]})\n")
            .append("**Duration:** ${p["nights"]} nights\n")
            .append("**Price:** from $${p["priceUsd"]} per person\n")
            .append("**Includes:** ${p["includes"]}\n\n")
        }
        return textResult(sb.toString())
    }

    /** Returns mocked availability for a destination in a given month.  */
    fun checkAvailability(destinationId: String?, month: String?, travelers: Int?): MutableMap<String?, Any?> {
        if (destinationId.isNullOrBlank()) {
            return textResult("Error: 'destination' parameter is required.", true)
        }
        if (month.isNullOrBlank()) {
            return textResult("Error: 'month' parameter is required.", true)
        }

        val key = destinationId.lowercase(Locale.getDefault()).replace(" ", "_")
        if (!destinations.containsKey(key)) {
            return textResult(
                "Destination '$destinationId' not found. Call list_destinations to see available options.",
                true
            )
        }

        val pax = if (travelers != null && travelers > 0) travelers else 2
        val isHighSeason = highSeasonMonths.contains(capitalize(month))
        val d: MutableMap<String, Any> = destinations[key]!!

        val bestMonths = d["bestMonths"] as MutableList<String>
        val isBestTime = bestMonths.stream().anyMatch { m: String -> m.equals(month, ignoreCase = true) }

        val spotsLeft = if (isHighSeason) "8 spots remaining (high season)" else "24 spots available"
        val surcharge = if (isHighSeason) "+20% high-season surcharge applies" else "No surcharges"
        val recommendation =
            if (isBestTime) "Excellent choice — this is prime season!" else "Travelling off-peak; expect fewer crowds but check weather."

        val sb = StringBuilder()
        sb.append("# Availability — ").append(d["name"]).append(" in ").append(capitalize(month)).append("\n\n")
        sb.append("**Travelers:** ").append(pax).append("\n")
        sb.append("**Status:** Available\n")
        sb.append("**Remaining spots:** ").append(spotsLeft).append("\n")
        sb.append("**Pricing note:** ").append(surcharge).append("\n")
        sb.append("**Tip:** ").append(recommendation).append("\n")
        return textResult(sb.toString())
    }


    // ─── Helpers ─────────────────────────────────────────────────────────────


    private fun pkg(
        name: String, destination: String,
        type: String, nights: Int, priceUsd: Int, includes: String
    ): MutableMap<String, Any> {
        val result: MutableMap<String, Any> = LinkedHashMap<String, Any>()
        result["name"] = name
        result["destination"] = destination
        result["type"] = type
        result["nights"] = nights
        result["priceUsd"] = priceUsd
        result["includes"] = includes
        return result
    }

    private fun textResult(text: String?): MutableMap<String?, Any?> {
        return textResult(text, false)
    }

    private fun textResult(text: String?, isError: Boolean): MutableMap<String?, Any?> {
        val content: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        content["type"] = "text"
        content["text"] = text
        val result: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        result["content"] = mutableListOf(content)
        if (isError) result["isError"] = true
        return result
    }

    private fun formatDestinationList(list: MutableList<MutableMap<String?, Any?>>): String {
        val sb = StringBuilder("# Available Holiday Destinations\n\n")
        for (item in list) {
            sb.append("- **").append(item["id"]).append("** — ")
                .append(item["name"]).append(": ").append(item["description"]).append("\n")
        }
        sb.append("\nUse `get_destination_info` with one of these IDs for full details.")
        return sb.toString()
    }

    private fun castList(obj: Any?): MutableList<String?>? {
        return obj as MutableList<String?>?
    }

    private fun capitalize(s: String?): String? {
        if (s.isNullOrEmpty()) return s
        return s[0].uppercaseChar().toString() + s.substring(1).lowercase(Locale.getDefault())
    }

}
