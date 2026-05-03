package org.evomaster.core.search.gene.regex

data class RegexFlags(
    // currently implemented
    val caseInsensitive: Boolean = false,        // i

    // recognised but not yet implemented, validate() throws on these
    val unicodeCase: Boolean = false,            // u, this flags modifies behaviour of "i" flag
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

    fun merge(toEnable: RegexFlags, toDisable: RegexFlags): RegexFlags {
        return RegexFlags(
            caseInsensitive       = merge(caseInsensitive,       toEnable.caseInsensitive,       toDisable.caseInsensitive),
            unicodeCase           = merge(unicodeCase,           toEnable.unicodeCase,           toDisable.unicodeCase),
            dotAll                = merge(dotAll,                toEnable.dotAll,                toDisable.dotAll),
            multiline             = merge(multiline,             toEnable.multiline,             toDisable.multiline),
            unixLines             = merge(unixLines,             toEnable.unixLines,             toDisable.unixLines),
            unicodeCharacterClass = merge(unicodeCharacterClass, toEnable.unicodeCharacterClass, toDisable.unicodeCharacterClass),
            comments              = merge(comments,              toEnable.comments,              toDisable.comments),
        )
    }

    private fun merge(current: Boolean, enable: Boolean, disable: Boolean) = when {
        disable -> false
        enable  -> true
        else    -> current
    }

    /**
     * Throws a clear error for any flag that is recognised in the grammar
     * but not yet implemented in the gene layer.
     * Call this after merging, before recursing into the flagged disjunction.
     */
    fun validate() {
        if (unicodeCase)                throw IllegalStateException("Regex flag 'u' (UNICODE_CASE) is not yet supported")
        if (dotAll)                throw IllegalStateException("Regex flag 's' (DOTALL) is not yet supported")
        if (multiline)             throw IllegalStateException("Regex flag 'm' (MULTILINE) is not yet supported")
        if (unixLines)             throw IllegalStateException("Regex flag 'd' (UNIX_LINES) is not yet supported")
        if (unicodeCharacterClass) throw IllegalStateException("Regex flag 'U' (UNICODE_CHARACTER_CLASS) is not yet supported")
        if (comments) throw IllegalStateException("Regex flag 'x' (COMMENTS) is not yet supported")
    }

    /**
     * Checks if the provided character has a case variant, checking caseInsensitive flag.
     */
    fun isCaseable(codePoint: Int): Boolean {
        return if (caseInsensitive) {
            codePoint in 0..127 && Character.toUpperCase(codePoint) != Character.toLowerCase(codePoint)
        }
        else {
            false
        }
    }
    /** @see org.evomaster.core.search.gene.regex.RegexFlags.isCaseable */
    fun isCaseable(char: Char): Boolean = isCaseable(char.code)
}