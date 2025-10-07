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
            Regex("""During handling of the above exception"""),
            Regex("""\b\w+(Error|Exception):"""), // Python exception types (ValueError, KeyError, etc.)

            // Java / Kotlin (JVM stack traces)
            Regex("""\w+\.\w+Exception:"""), // Java/Kotlin exception types (e.g., java.lang.NullPointerException)
            Regex("""\bat [a-zA-Z0-9_\.$]+\(.*:\d+\)"""),
            Regex("""Caused by:"""),
            Regex("""Suppressed:"""),
            Regex("""\.\.\. \d+ more"""),

            // C#
            Regex("""System\.\w+Exception:"""), // System.Exception types
            Regex("""\bat [\w\.]+\(.*\) in .+:line \d+"""),
            Regex("""--- End of (stack trace|inner exception stack trace) from previous location ---"""),
            Regex("""---> .+Exception:"""),
            Regex("""\bat [\w\.<>]+\.MoveNext\(\)"""), // async/await pattern
            Regex("""\bat System\.Runtime\."""), // System.Runtime stack frames

            // JavaScript / Node.js (V8 engine stack traces)
            Regex("""\b(TypeError|ReferenceError|Error|SyntaxError|RangeError):"""), // JS exception types
            Regex("""\bat .+\(.+:\d+:\d+\)"""),
            Regex("""\bat .+:\d+:\d+"""),
            Regex("""\bat async """),
            Regex("""Uncaught \w+Error:"""),

            // Ruby
            Regex("""\b.+:\d+:in `[^`]+`"""),
            Regex("""\b\w+::\w+(Error|Exception):"""), // Ruby exception types

            // Go
            Regex("""\b[\w./-]+\.go:\d+\b"""),
            Regex("""goroutine \d+ \["""),
            Regex("""created by """),

            // PHP
            Regex("""^#\d+\s+.*\.php\(\d+\):""", setOf(RegexOption.MULTILINE)),
            Regex("""Next \w+Exception:"""),
            Regex("""thrown in .+ on line \d+"""),
            Regex("""(Fatal error|PHP Fatal error):.*Exception:"""), // PHP fatal errors with exceptions
            Regex("""\bPDOException:"""), // PHP PDO exceptions

            // Rust
            Regex("""thread '.+' panicked at"""),
            Regex("""stack backtrace:"""),

            // Scala
            Regex("""scala\.\w+:"""), // Scala exception types

            // Dart/Flutter
            Regex("""^#\d+\s+.+\(package:.+\.dart:\d+:\d+\)""", setOf(RegexOption.MULTILINE)),

            // Elixir
            Regex("""\*\* \(\w+\)"""), // Elixir exception format

            // Swift
            Regex("""Fatal error:"""),

            // Generic file:line matcher for many languages
            Regex("""\b[a-zA-Z0-9_\-/\\\.]+\.(java|kt|py|cs|js|ts|rb|go|php|cpp|c|m|mm|rs|swift|dart|ex|scala):\d+\b""")
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

            // First, normalize escaped newlines (e.g., from JSON strings)
            val normalizedText = text.replace("\\n", "\n").replace("\\r", "\r")

            // Extract all string values from JSON if text appears to be JSON
            // This handles: {"trace": "..."}, {"stack": ["...", "..."]}, nested objects, etc.
            val textToAnalyze = if (looksLikeJson(normalizedText)) {
                extractAllStringsFromJson(normalizedText)
            } else {
                normalizedText
            }

            // Check if any error keyword is present in the entire text
            val keywordPresent = errorKeywords.any { kw ->
                textToAnalyze.contains(kw, ignoreCase = true)
            }

            // Count how many lines match known stack trace patterns
            var matchCount = 0
            for (line in textToAnalyze.lineSequence()) {
                if (patterns.any { it.containsMatchIn(line) }) {
                    matchCount++
                    if (matchCount >= 3) break // early stop for efficiency
                }
            }

            // Decision rule
            return (keywordPresent && matchCount >= 2) || (!keywordPresent && matchCount >= 3)
        }

        /**
         * Simple heuristic to detect if text looks like JSON
         */
        private fun looksLikeJson(text: String): Boolean {
            val trimmed = text.trim()
            return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                   (trimmed.startsWith("[") && trimmed.endsWith("]"))
        }

        /**
         * Extract all string values from JSON (including arrays and nested objects)
         * and combine them with newlines. This is a simple regex-based approach that
         * works for most cases without requiring a full JSON parser.
         */
        private fun extractAllStringsFromJson(json: String): String {
            // Match all quoted strings in JSON, handling escaped quotes
            val stringPattern = Regex(""""((?:[^"\\]|\\.)*)"""")
            val matches = stringPattern.findAll(json)

            return matches
                .map { it.groupValues[1] }
                .joinToString("\n")
        }
}