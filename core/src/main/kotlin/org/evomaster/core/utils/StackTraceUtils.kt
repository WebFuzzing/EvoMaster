package org.evomaster.core.utils

/**
 * Utility for detecting stack traces in text, including JSON-embedded traces.
 * Supports multiple programming languages and frameworks commonly used in RESTful APIs.
 */
object StackTraceUtils {

    // Minimum pattern matches required for detection
    private const val MIN_MATCHES_WITH_KEYWORD = 2
    private const val MIN_MATCHES_WITHOUT_KEYWORD = 3

    // Common keywords that usually appear in stack traces or error messages
    private val ERROR_KEYWORDS = listOf(
        "exception", "error", "throwable", "traceback", "caused by", "panic"
    )

    /**
     * Determines whether the given text looks like a stack trace.
     *
     * Detection heuristics:
     * - If error/exception keyword present AND at least 2 pattern matches → true
     * - If no keyword but at least 3 pattern matches → true
     *
     * Supports:
     * - Direct stack traces (plain text)
     * - JSON-embedded stack traces (with escaped newlines)
     * - Multiple programming languages (Java, Python, C#, JavaScript, Go, PHP, Ruby, Rust, etc.)
     *
     * @param text the log or string to analyze
     * @return true if the text appears to be a stack trace
     */
    fun looksLikeStackTrace(text: String): Boolean {
        if (text.isBlank()) return false

        val normalizedText = normalizeEscapeSequences(text)
        val textToAnalyze = if (looksLikeJson(normalizedText)) {
            extractAndNormalizeFromJson(normalizedText)
        } else {
            normalizedText
        }

        val keywordPresent = containsErrorKeyword(textToAnalyze)
        val matchCount = countPatternMatches(textToAnalyze)

        return (keywordPresent && matchCount >= MIN_MATCHES_WITH_KEYWORD) ||
               (!keywordPresent && matchCount >= MIN_MATCHES_WITHOUT_KEYWORD)
    }

    /**
     * Normalizes escape sequences in text (handles \n, \r, \t, \\)
     */
    private fun normalizeEscapeSequences(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    /**
     * Normalizes escape sequences iteratively to handle multiple levels of escaping
     */
    private fun normalizeEscapeSequencesIteratively(text: String): String {
        var normalized = text
        var previous: String

        do {
            previous = normalized
            normalized = normalized
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
        } while (normalized != previous)

        return normalized
    }

    /**
     * Checks if text looks like JSON
     */
    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }

    /**
     * Checks if text contains any error keywords
     */
    private fun containsErrorKeyword(text: String): Boolean {
        return ERROR_KEYWORDS.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Counts how many lines match stack trace patterns
     */
    private fun countPatternMatches(text: String): Int {
        var count = 0
        for (line in text.lineSequence()) {
            if (StackTracePatterns.matchesAny(line)) {
                count++
                if (count >= MIN_MATCHES_WITHOUT_KEYWORD) {
                    break // Early optimization
                }
            }
        }
        return count
    }

    /**
     * Extracts content from JSON and normalizes it
     */
    private fun extractAndNormalizeFromJson(json: String): String {
        val extracted = JsonExtractor.extractAllContent(json)
        return normalizeEscapeSequencesIteratively(extracted)
    }

    /**
     * Patterns for detecting stack traces from various programming languages
     */
    private object StackTracePatterns {

        private val pythonPatterns = listOf(
            Regex("""Traceback \(most recent call last\):""", RegexOption.IGNORE_CASE),
            Regex("""^\s*File ".*", line \d+"""),
            Regex("""During handling of the above exception"""),
            Regex("""\b\w+(Error|Exception):""")
        )

        private val jvmPatterns = listOf(
            Regex("""[\w\.]+Exception:"""),
            Regex("""^\s*at [a-zA-Z0-9_\.$<>]+\(.*:\d+\)"""),
            Regex("""^\s*at [a-zA-Z0-9_\.$<>]+\(.*\)$"""),
            Regex("""Caused by:"""),
            Regex("""Suppressed:"""),
            Regex("""\.\.\. \d+ more""")
        )

        private val csharpPatterns = listOf(
            Regex("""System\.[\w\.]+Exception:"""),
            Regex("""^\s*at [\w\.`<>]+(\[[\w,\s]+\])?\(.*\) in .+:line \d+"""),
            Regex("""^\s*at [\w\.`<>]+(\[[\w,\s]+\])?\(.*\)$"""),
            Regex("""--- End of (stack trace|inner exception stack trace) from previous location ---"""),
            Regex("""---> .+Exception:"""),
            Regex("""^\s*at [\w\.`<>]+\.MoveNext\(\)"""),
            Regex("""^\s*at System\.Runtime\.""")
        )

        private val javascriptPatterns = listOf(
            Regex("""\b(TypeError|ReferenceError|Error|SyntaxError|RangeError):"""),
            Regex("""^\s*at .+\(.+:\d+:\d+\)"""),
            Regex("""^\s*at .+:\d+:\d+"""),
            Regex("""^\s*at async """),
            Regex("""Uncaught \w+Error:""")
        )

        private val rubyPatterns = listOf(
            Regex("""^\s*.+:\d+:in `[^`]+`"""),
            Regex("""[\w:]+::\w+(Error|Exception):""")
        )

        private val goPatterns = listOf(
            Regex("""\b[\w./-]+\.go:\d+\b"""),
            Regex("""goroutine \d+ \["""),
            Regex("""created by """)
        )

        private val phpPatterns = listOf(
            Regex("""^#\d+\s+.*\.php\(\d+\):""", setOf(RegexOption.MULTILINE)),
            Regex("""[\w\\]+->[\w]+\("""),
            Regex("""Next \w+Exception:?"""),
            Regex("""thrown in .+ on line \d+"""),
            Regex("""(Fatal error|PHP Fatal error):.*Exception:?"""),
            Regex("""\bPDOException:?"""),
            Regex("""[\w\\]+Exception""")
        )

        private val rustPatterns = listOf(
            Regex("""thread '.+' panicked at"""),
            Regex("""stack backtrace:""")
        )

        private val scalaPatterns = listOf(
            Regex("""scala\.[\w\.]+:""")
        )

        private val dartPatterns = listOf(
            Regex("""^#\d+\s+.+\(package:.+\.dart:\d+:\d+\)""", setOf(RegexOption.MULTILINE))
        )

        private val elixirPatterns = listOf(
            Regex("""\*\* \(\w+\)""")
        )

        private val swiftPatterns = listOf(
            Regex("""Fatal error:""")
        )

        private val genericPatterns = listOf(
            Regex("""\b[a-zA-Z0-9_\-/\\\.]+\.(java|kt|py|cs|js|ts|rb|go|php|cpp|c|m|mm|rs|swift|dart|ex|scala):\d+\b""")
        )

        private val allPatterns = pythonPatterns + jvmPatterns + csharpPatterns +
                                  javascriptPatterns + rubyPatterns + goPatterns +
                                  phpPatterns + rustPatterns + scalaPatterns +
                                  dartPatterns + elixirPatterns + swiftPatterns +
                                  genericPatterns

        fun matchesAny(line: String): Boolean {
            return allPatterns.any { it.containsMatchIn(line) }
        }
    }

    /**
     * Extracts strings and reconstructs patterns from JSON
     */
    private object JsonExtractor {

        fun extractAllContent(json: String): String {
            val strings = mutableListOf<String>()

            strings.addAll(extractStringValues(json))
            strings.addAll(extractFieldNames(json))
            strings.addAll(reconstructFileLinePatterns(json))
            strings.addAll(reconstructClassFunctionPatterns(json))

            return strings.joinToString("\n")
        }

        /**
         * Extracts all quoted string values from JSON
         */
        private fun extractStringValues(json: String): List<String> {
            val pattern = Regex(""""((?:[^"\\]|\\.)*)"""", RegexOption.DOT_MATCHES_ALL)
            return pattern.findAll(json)
                .map { it.groupValues[1] }
                .toList()
        }

        /**
         * Extracts field names (useful for detecting "error", "stack", "trace" keywords)
         */
        private fun extractFieldNames(json: String): List<String> {
            val pattern = Regex(""""(\w+)"\s*:""")
            return pattern.findAll(json)
                .map { it.groupValues[1] }
                .toList()
        }

        /**
         * Reconstructs file:line patterns from separate fields
         * Example: {"file": "app.php", "line": 42} -> "app.php:42"
         */
        private fun reconstructFileLinePatterns(json: String): List<String> {
            val pattern = Regex(""""file"\s*:\s*"([^"]+)"[^}]*"line"\s*:\s*(\d+)""")
            return pattern.findAll(json)
                .map { "${it.groupValues[1]}:${it.groupValues[2]}" }
                .toList()
        }

        /**
         * Reconstructs class->function patterns from PHP trace objects
         * Example: {"class": "App\\User", "function": "getName"} -> "App\User->getName()"
         */
        private fun reconstructClassFunctionPatterns(json: String): List<String> {
            val pattern = Regex(""""class"\s*:\s*"([^"]+)"[^}]*"function"\s*:\s*"([^"]+)"""")
            return pattern.findAll(json)
                .map { "${it.groupValues[1]}->${it.groupValues[2]}()" }
                .toList()
        }
    }
}
