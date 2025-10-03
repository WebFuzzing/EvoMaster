package org.evomaster.core.utils

object StackTraceUtils {

        // Common keywords that usually appear in stack traces or error messages
        private val errorKeywords = listOf(
            "exception", "error", "throwable", "traceback", "caused by", "panic"
        )

        // Regex patterns to match stack-trace-like lines from different languages
        private val patterns: List<Regex> = listOf(
            // Python
            Regex("""Traceback \(most recent call last\):""", RegexOption.IGNORE_CASE),
            Regex("""File ".*", line \d+"""),

            // Java / Kotlin (JVM stack traces)
            Regex("""\bat [a-zA-Z0-9_\.$]+\(.*:\d+\)"""),

            // C#
            Regex("""\bat [\w\.]+\(.*\) in .+:line \d+"""),

            // JavaScript / Node.js (V8 engine stack traces)
            Regex("""\bat .+\(.+:\d+:\d+\)"""),
            Regex("""\bat .+:\d+:\d+"""),

            // Ruby
            Regex("""\b.+:\d+:in `[^`]+`"""),

            // Go
            Regex("""\b[\w./-]+\.go:\d+\b"""),

            // PHP
            Regex("""^#\d+\s+.*\.php\(\d+\):""", setOf(RegexOption.MULTILINE)),

            // Generic file:line matcher for many languages
            Regex("""\b[a-zA-Z0-9_\-/\\\.]+\.(java|kt|py|cs|js|ts|rb|go|php|cpp|c|m|mm):\d+\b""")
        )

        /**
         * Determines whether the given text looks like a stack trace.
         *
         * Heuristics:
         * - If there is an error/exception keyword AND at least 2 matching lines → true
         * - If there is no keyword but at least 3 matching lines → true
         *
         * @param text the log or string to analyze
         * @return true if the text appears to be a stack trace
         */
        fun looksLikeStackTrace(text: String): Boolean {
            if (text.isBlank()) return false

            // Check if any error keyword is present in the entire text
            val keywordPresent = errorKeywords.any { kw ->
                text.contains(kw, ignoreCase = true)
            }

            // Count how many lines match known stack trace patterns
            var matchCount = 0
            for (line in text.lineSequence()) {
                if (patterns.any { it.containsMatchIn(line) }) {
                    matchCount++
                    if (matchCount >= 3) break // early stop for efficiency
                }
            }

            // Decision rule
            return (keywordPresent && matchCount >= 2) || (!keywordPresent && matchCount >= 3)
        }
}