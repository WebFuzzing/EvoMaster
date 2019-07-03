package org.evomaster.core.database

class PostgresToJavaRegExTranslator {

    /**
     * Translates postgres wildcards to their corresponding
     * Java regex wildcard. This is, "_" -> ".", "%" -> ".*".
     * If a wildcard is escaped with \, it is not replaced.
     * For example, "\%" -> "%", "\_" -> "_", "\\" -> "\"
     *
     * Reference: https://www.postgresql.org/docs/9.0/functions-matching.html#FUNCTIONS-LIKE
     */
    fun translatePostgresLike(likePattern: String): String {
        // TODO Escape SQL regular expressions and Java Regex regular expressions symbols
        val escapeSymbol: String = "\\"
        val replacements = mapOf("%" to ".*", "_" to ".")
        val builder = StringBuilder()
        var i = 0
        while (i < likePattern.length) {
            val currentSymbol = likePattern[i].toString()
            if (currentSymbol == escapeSymbol && i < likePattern.length - 1) {
                i++;
                builder.append(likePattern[i])
            } else if (currentSymbol in replacements.keys) {
                builder.append(replacements[currentSymbol])
            } else {
                builder.append(currentSymbol)
            }
            i++;
        }
        return builder.toString()
    }

    /**
     * Translates the SIMILAR TO pattern to a Java Regex pattern. It is similar to translating LIKE patterns,
     * except that SQL standard's definition of a regular expression must be mapped.
     * SQL regular expressions are a curious cross between LIKE notation and common regular expression notation.
     *
     * | denotes alternation (either of two alternatives).
     * * denotes repetition of the previous item zero or more times.
     *  + denotes repetition of the previous item one or more times.
     * ? denotes repetition of the previous item zero or one time.
     * {m} denotes repetition of the previous item exactly m times.
     * {m,} denotes repetition of the previous item m or more times.
     * {m,n} denotes repetition of the previous item at least m and not more than n times.
     * Parentheses () can be used to group items into a single logical item.
     *  A bracket expression [...] specifies a character class, just as in POSIX regular expressions.
     *
     * Reference: https://www.postgresql.org/docs/9.0/functions-matching.html#FUNCTIONS-SIMILARTO-REGEXP
     */
    fun translatePostgresSimilarTo(similarToPattern: String): String {
        // TODO Translate SQL standard regular expression symbols into Java Regex symbols
        return translatePostgresLike(similarToPattern)
    }

    /**
     * Translates a posix pattern from a ~ operation into its corresponding java regex pattern
     * Reference: https://www.postgresql.org/docs/9.0/functions-matching.html#FUNCTIONS-POSIX-REGEXP
     */
    fun translatePostgresPosix(posixPattern: String, caseSensitive: Boolean = true): String {
        // TODO: add handling of insensitive case
        return translatePostgresSimilarTo(posixPattern)
    }

}