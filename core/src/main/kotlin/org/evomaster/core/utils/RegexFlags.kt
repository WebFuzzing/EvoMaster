package org.evomaster.core.utils

/**
 * Represents a parsed flag expression like "(?iu-s:".
 * Encapsulates the flags being turned on and off, to be applied to an existing [RegexFlags] via [RegexFlags.merge].
 */
data class ParsedFlagExpression(
    private val toEnable: RegexFlags,
    private val toDisable: RegexFlags
) {
    internal fun applyTo(current: RegexFlags): RegexFlags = RegexFlags(
        caseInsensitive       = merge(current.caseInsensitive,       toEnable.caseInsensitive,       toDisable.caseInsensitive),
        unicodeCase           = merge(current.unicodeCase,           toEnable.unicodeCase,           toDisable.unicodeCase),
        dotAll                = merge(current.dotAll,                toEnable.dotAll,                toDisable.dotAll),
        multiline             = merge(current.multiline,             toEnable.multiline,             toDisable.multiline),
        unixLines             = merge(current.unixLines,             toEnable.unixLines,             toDisable.unixLines),
        unicodeCharacterClass = merge(current.unicodeCharacterClass, toEnable.unicodeCharacterClass, toDisable.unicodeCharacterClass),
        comments              = merge(current.comments,              toEnable.comments,              toDisable.comments),
    )

    private fun merge(current: Boolean, enable: Boolean, disable: Boolean) = when {
        disable -> false
        enable  -> true
        else    -> current
    }
}

data class RegexFlags(
    // currently implemented
    val caseInsensitive: Boolean = false,        // i
    val unicodeCase: Boolean = false,            // u, this flags modifies behaviour of "i" flag

    // recognised but not yet implemented, validate() throws on these
    val dotAll: Boolean = false,                 // s
    val multiline: Boolean = false,              // m
    val unixLines: Boolean = false,              // d
    val unicodeCharacterClass: Boolean = false,  // U
    val comments: Boolean = false,               // x
) {

    companion object {
        /**
         * Parses a string of flag characters (e.g. "iu", "sm") into a [RegexFlags] instance.
         * Valid characters are: i, u, s, m, d, U, x.
         */
        fun fromString(s: String): RegexFlags {
            require(s.all { c -> c in "iusmdUx" }) { "Invalid flag characters in: '$s'" }
            return RegexFlags(
                caseInsensitive       = 'i' in s,
                unicodeCase           = 'u' in s,
                dotAll                = 's' in s,
                multiline             = 'm' in s,
                unixLines             = 'd' in s,
                unicodeCharacterClass = 'U' in s,
                comments              = 'x' in s,
            )
        }
    }

    /**
     * Merges this [RegexFlags] with a [ParsedFlagExpression], returning a new [RegexFlags] with the
     * enabled flags turned on and the disabled flags turned off.
     * Flags not mentioned in either are inherited from the receiver unchanged.
     */
    fun merge(expression: ParsedFlagExpression): RegexFlags = expression.applyTo(this)

    /**
     * Throws a clear error for any flag that is recognised in the grammar
     * but not yet implemented in the gene layer.
     * Call this after merging, before recursing into the flagged disjunction.
     */
    fun validate() {
        if (dotAll)                throw IllegalStateException("Regex flag 's' (DOTALL) is not yet supported")
        if (multiline)             throw IllegalStateException("Regex flag 'm' (MULTILINE) is not yet supported")
        if (unixLines)             throw IllegalStateException("Regex flag 'd' (UNIX_LINES) is not yet supported")
        if (unicodeCharacterClass) throw IllegalStateException("Regex flag 'U' (UNICODE_CHARACTER_CLASS) is not yet supported")
        if (comments) throw IllegalStateException("Regex flag 'x' (COMMENTS) is not yet supported")
    }

    /**
     * Checks if the provided character has a case variant according to the flag behavior, checking both caseInsensitive
     * and unicodeCase flag values.
     */
    fun isCaseable(codePoint: Int): Boolean {
        return if (caseInsensitive && unicodeCase) {
            Character.toUpperCase(codePoint) != Character.toLowerCase(codePoint)
        }
        else if (caseInsensitive) {
            codePoint in 0..127 && Character.toUpperCase(codePoint) != Character.toLowerCase(codePoint)
        }
        else {
            false
        }
    }
    /** @see org.evomaster.core.utils.RegexFlags.isCaseable */
    fun isCaseable(char: Char): Boolean = isCaseable(char.code)
}