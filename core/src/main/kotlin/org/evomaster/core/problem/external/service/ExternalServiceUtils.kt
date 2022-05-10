package org.evomaster.core.problem.external.service

object ExternalServiceUtils {

    fun nextIPAddress(address: String) : String {
        val tokens = address.split(".").toMutableList()
        if (tokens.size != 4) {
            throw IllegalArgumentException("Invalid IP Address")
        } else {
            for ( i in tokens.size - 1 downTo 0) {
                var part = tokens[i].toInt()
                if (part < 255) {
                    part += 1
                    tokens[i] = part.toString()
                    for (j in i + 1 until tokens.size) {
                        tokens[j] = "0"
                    }
                    break
                }
            }

        }
        return String.format("%s.%s.%s.%s", tokens[0], tokens[1], tokens[2], tokens[3])
    }
}