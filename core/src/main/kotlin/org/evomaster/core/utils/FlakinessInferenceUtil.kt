package org.evomaster.core.utils

/**
 * This utility is used to infer possible flaky property based on actual value using regex
 */
object FlakinessInferenceUtil {

    // ===== markers =====
    const val TIME_MARKER   = "_EM_POTENTIAL_TIME_FLAKINESS_"
    const val UUID_MARKER   = "_EM_POTENTIAL_UUID_FLAKINESS_"
    const val OBJ_MARKER    = "_EM_POTENTIAL_OBJECT_FLAKINESS_"
    const val HASH_MARKER   = "_EM_POTENTIAL_CRYPTO_FLAKINESS_"
    const val RAND_MARKER   = "_EM_POTENTIAL_RANDOM_FLAKINESS_"
    const val RUNMSG_MARKER = "_EM_POTENTIAL_RUNMSG_FLAKINESS_"

    // ===== time regex =====
    /**
     * Match quoted ISO-like date/time strings (full with millis, without millis, or date-only).
     */
    private val TIME_STRING_REGEX = Regex(
        "\"(" +
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z" +
                "|" +
                "\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}" +
                "|" +
                "\\d{4}-\\d{2}-\\d{2}" +
                ")\""
    )

    /**
     * Match 13-digit epoch milliseconds.
     */
    private val EPOCH_REGEX = Regex("\\b\\d{13}\\b")

    // ===== randomness regex =====
    /**
     * Match UUID strings.
     */
    private val UUID_REGEX = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{12}"
    )

    /**
     * Match long hex tokens (length >= 16).
     */
    private val RANDOM_HEX_REGEX = Regex("\\b[0-9a-fA-F]{16,}\\b")

    /**
     * Match base64-looking tokens (length >= 20), with optional padding.
     */
    private val BASE64_REGEX = Regex("\\b[A-Za-z0-9+/]{20,}={0,2}\\b")

    // ===== crypto regex =====
    /**
     * Match common hash lengths (MD5/SHA1/SHA256).
     */
    private val HASH_REGEX = Regex(
        "\\b[a-fA-F0-9]{32}\\b|" +
                "\\b[a-fA-F0-9]{40}\\b|" +
                "\\b[a-fA-F0-9]{64}\\b"
    )

    // ===== runtime message regex =====
    /**
     * Match JVM object toString like com.foo.Bar@1a2b3c.
     */
    private val OBJECT_AT_REGEX = Regex("[A-Za-z0-9_.$]+@[0-9a-fA-F]+")

    /**
     * Match hex pointers like 0x1a2b3c.
     */
    private val POINTER_REGEX = Regex("0x[0-9a-fA-F]+")

    /**
     * Match Java line numbers like .java:123.
     */
    private val LINE_NUMBER_REGEX = Regex("\\.java:\\d+")

    /**
     * infer potential flaky value using predefined regex
     * We may normalize the responses for clear flaky source instead of commenting them out
     * @return identified flaky mark
     */
    fun derive(body: String): String {

        var result = body

        // handle time
        result = TIME_STRING_REGEX.replace(result) { "\"$TIME_MARKER\"" }
        result = EPOCH_REGEX.replace(result, TIME_MARKER)

        // handle cryptographic hash
        result = HASH_REGEX.replace(result, HASH_MARKER)

        // handle randomness
        result = UUID_REGEX.replace(result, UUID_MARKER)
        result = RANDOM_HEX_REGEX.replace(result, RAND_MARKER)
        result = BASE64_REGEX.replace(result, RAND_MARKER)

        // handle runtime message
        result = OBJECT_AT_REGEX.replace(result, OBJ_MARKER)
        result = POINTER_REGEX.replace(result, RUNMSG_MARKER)
        result = LINE_NUMBER_REGEX.replace(result, RUNMSG_MARKER)

        return result
    }
}
