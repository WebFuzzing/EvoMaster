package org.evomaster.core.database

class PostgresToJavaRegExTranslator {

    /**
     * Translates postgres wildcards to their corresponding
     * Java regex wildcard. This is, "_" -> ".", "%" -> ".*".
     * If a wildcard is escaped with \, it is not replaced.
     * For example, "\%" -> "%", "\_" -> "_", "\\" -> "\"
     */
    fun translate(postgresPattern: String): String {
        val escapeSymbol: String = "\\"
        val replacements = mapOf("%" to ".*", "_" to ".")
        val builder = StringBuilder()
        var i = 0
        while (i < postgresPattern.length) {
            val currentSymbol = postgresPattern[i].toString()
            if (currentSymbol == escapeSymbol && i < postgresPattern.length - 1) {
                i++;
                builder.append(postgresPattern[i])
            } else if (currentSymbol in replacements.keys) {
                builder.append(replacements[currentSymbol])
            } else {
                builder.append(currentSymbol)
            }
            i++;
        }
        return builder.toString()
    }
}