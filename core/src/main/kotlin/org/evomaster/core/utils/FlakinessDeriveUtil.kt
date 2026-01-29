package org.evomaster.core.utils

object FlakinessDeriveUtil {

    // ===== markers =====

    private const val TIME_MARKER   = "_EM_POTENTIAL_TIME_FLAKINESS_"
    private const val UUID_MARKER   = "_EM_POTENTIAL_UUID_FLAKINESS_"
    private const val OBJ_MARKER    = "_EM_POTENTIAL_OBJECT_FLAKINESS_"
    private const val HASH_MARKER   = "_EM_POTENTIAL_CRYPTO_FLAKINESS_"
    private const val RAND_MARKER   = "_EM_POTENTIAL_RANDOM_FLAKINESS_"
    private const val RUNMSG_MARKER = "_EM_POTENTIAL_RUNMSG_FLAKINESS_"

    // ===== time regex =====

    private val TIME_STRING_REGEX = Regex(
        "\"(" +
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z" +
                "|" +
                "\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}" +
                "|" +
                "\\d{4}-\\d{2}-\\d{2}" +
                ")\""
    )

    private val EPOCH_REGEX = Regex("\\b\\d{13}\\b")

    // ===== randomness regex =====

    private val UUID_REGEX = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{12}"
    )

    private val RANDOM_HEX_REGEX = Regex("\\b[0-9a-fA-F]{16,}\\b")

    private val BASE64_REGEX = Regex("\\b[A-Za-z0-9+/]{20,}={0,2}\\b")

    // ===== crypto regex =====

    private val HASH_REGEX = Regex(
        "\\b[a-fA-F0-9]{32}\\b|" +
                "\\b[a-fA-F0-9]{40}\\b|" +
                "\\b[a-fA-F0-9]{64}\\b"
    )

    // ===== runtime message regex =====

    private val OBJECT_AT_REGEX = Regex("[A-Za-z0-9_.$]+@[0-9a-fA-F]+")

    private val POINTER_REGEX = Regex("0x[0-9a-fA-F]+")

    private val LINE_NUMBER_REGEX = Regex("\\.java:\\d+")


    // ===== public API =====

    fun derive(body: String): String {

        var result = body

        // handle time
        result = TIME_STRING_REGEX.replace(result) { "\"$TIME_MARKER\"" }
        result = EPOCH_REGEX.replace(result, TIME_MARKER)

        // handle randomness
        result = UUID_REGEX.replace(result, UUID_MARKER)
        result = RANDOM_HEX_REGEX.replace(result, RAND_MARKER)
        result = BASE64_REGEX.replace(result, RAND_MARKER)

        // handle cryptographic hash
        result = HASH_REGEX.replace(result, HASH_MARKER)

        // handle runtime message
        result = OBJECT_AT_REGEX.replace(result, OBJ_MARKER)
        result = POINTER_REGEX.replace(result, RUNMSG_MARKER)
        result = LINE_NUMBER_REGEX.replace(result, RUNMSG_MARKER)

        return result
    }
}
