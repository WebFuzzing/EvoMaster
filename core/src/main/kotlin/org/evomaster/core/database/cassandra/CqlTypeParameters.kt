package org.evomaster.core.database.cassandra

/**
 * Splitting of a CQL fragment on a separator, ignoring the separators nested inside a type
 * parameter list, as a collection type is itself written with them, eg "map<text, int>".
 */
internal object CqlTypeParameters {

    const val START = '<'

    const val END = '>'

    /**
     * @param text the fragment to split
     * @param separator the character to split [text] on, when not nested inside a type parameter list
     * @return the parts of [text], in the same order, not trimmed
     * @throws IllegalArgumentException if the type parameter lists in [text] are not balanced, as
     * then there is no telling which of the separators are the ones being split on
     */
    fun splitAtTopLevel(text: String, separator: Char): List<String> {

        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0

        for (c in text) {
            when {
                c == START -> {
                    depth++
                    current.append(c)
                }
                c == END -> {
                    if (depth == 0) {
                        throw IllegalArgumentException(unbalancedMessage(text))
                    }
                    depth--
                    current.append(c)
                }
                c == separator && depth == 0 -> {
                    parts.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }

        if (depth != 0) {
            throw IllegalArgumentException(unbalancedMessage(text))
        }

        parts.add(current.toString())

        return parts
    }

    private fun unbalancedMessage(text: String) = "Unbalanced type parameters in a CQL fragment: $text"
}