package org.evomaster.core.problem.rest.arazzo.parser

class SimpleConditionParser(condition: String) {
    private val tokens: List<String>
    private var currentPosition = 0

    init {
        // Esta Regex identifica todo el vocabulario válido de Arazzo
        val arazzoRegex = Regex(
            """\$[a-zA-Z0-9_\.\[\]]+|""" + // Runtime variables
                    """'([^']|'')*'|""" +          // Strings
                    """-?\d+(\.\d+)?|""" +         // Numbers
                    """true|false|null|""" +       // Literals
                    """==|!=|<=|>=|<|>|""" +       // Operators
                    """&&|\|\||!|""" +             // Logical Operators
                    """\(|\)"""                    // Parenthesis
        )
        tokens = arazzoRegex.findAll(condition).map { it.value }.toList()
    }

    private fun peek(): String? = tokens.getOrNull(currentPosition)
    private fun advance(): String? = tokens.getOrNull(currentPosition++)
    private fun match(vararg expected: String): Boolean {
        if (peek() in expected) {
            advance()
            return true
        }
        return false
    }

    fun validateOrThrow(): Boolean {
        if (tokens.isEmpty()) throw IllegalArgumentException("Empty expression")

        parseOrExpression()

        if (currentPosition != tokens.size) {
            throw IllegalArgumentException("Raw tokens")
        }

        return true
    }

    // OR Operator (||)
    private fun parseOrExpression() {
        parseAndExpression()
        while (match("||")) {
            parseAndExpression()
        }
    }

    // AND Operator (&&)
    private fun parseAndExpression() {
        parseComparison()
        while (match("&&")) {
            parseComparison()
        }
    }

    // Comparison Operators (==, !=, <, etc.)
    private fun parseComparison() {
        parseTerm()
        if (match("==", "!=", "<=", ">=", "<", ">")) {
            parseTerm()
        }
    }

    // Terms
    private fun parseTerm() {
        val token = peek() ?: throw IllegalArgumentException("Unexpected end of expression")

        when {
            match("!") -> parseTerm()
            match("(") -> {
                parseOrExpression()
                if (!match(")")) throw IllegalArgumentException("Unclosed parentheses")
            }
            isPrimitiveValue(token) -> {
                advance()
            }
            else -> throw IllegalArgumentException("Unexpected or invalid token: $token")
        }
    }

    // PrimitiveValue
    private fun isPrimitiveValue(token: String): Boolean {
        if (token.startsWith("'") ||
            token in listOf("true", "false", "null") ||
            token.matches(Regex("-?\\d+(\\.\\d+)?"))) {
            return true
        }

        if (token.startsWith("$")) {
            try {
                ExpressionParser.parse(token)
                return true
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid condition: $token", e)
            }
        }

        return false
    }
}