package org.evomaster.client.java.sql.heuristic;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SqlToJavaRegexTranslator {

    /**
     * Translates PostgreSQL wildcards to their corresponding
     * Java regex wildcard. This is, "_" to ".", "%" to ".*".
     * If a wildcard is escaped with \, it is not replaced.
     * For example, "\%" to "%", "\_" to "_", "\\" to "\"
     *
     * @param likePattern the SQL LIKE pattern to translate
     * @return the translated Java regex pattern
     * @throws NullPointerException if the likePattern is null
     *
     * Reference: https://www.postgresql.org/docs/9.0/functions-matching.html#FUNCTIONS-LIKE
     */
    public String translateLikePattern(String likePattern) {
        Objects.requireNonNull(likePattern);

        // TODO Escape SQL regular expressions and Java Regex regular expressions symbols
        String escapeSymbol = "\\";
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%", ".*");
        replacements.put("_", ".");

        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (i < likePattern.length()) {
            String currentSymbol = String.valueOf(likePattern.charAt(i));
            if (currentSymbol.equals(escapeSymbol) && i < likePattern.length() - 1) {
                i++;
                builder.append(likePattern.charAt(i));
            } else if (replacements.containsKey(currentSymbol)) {
                builder.append(replacements.get(currentSymbol));
            } else {
                builder.append(currentSymbol);
            }
            i++;
        }
        return builder.toString();
    }

    /**
     * Translates the SIMILAR TO pattern to a Java Regex pattern. It is similar to translating LIKE patterns,
     * except that the SQL standard's definition of a regular expression must be mapped.
     * SQL regular expressions are a curious cross between LIKE notation and common regular expression notation.
     *
     * | denotes alternation (either of two alternatives).
     * * denotes repetition of the previous item zero or more times.
     * + denotes repetition of the previous item one or more times.
     * ? denotes repetition of the previous item zero or one time.
     * {m} denotes repetition of the previous item exactly m times.
     * {m,} denotes repetition of the previous item m or more times.
     * {m,n} denotes repetition of the previous item at least m and not more than n times.
     * Parentheses () can be used to group items into a single logical item.
     * A bracket expression [...] specifies a character class, just as in POSIX regular expressions.
     *
     * @param similarToPattern the SQL SIMILAR TO pattern to translate
     * @return the translated Java regex pattern
     * @throws NullPointerException if the similarToPattern is null
     *
     * Reference: https://www.postgresql.org/docs/9.0/functions-matching.html#FUNCTIONS-SIMILARTO-REGEXP
     */
    public String translateSimilarToPattern(String similarToPattern) {
        Objects.requireNonNull(similarToPattern);
        // TODO Translate SQL standard regular expression symbols into Java Regex symbols
        return translateLikePattern(similarToPattern);
    }

    /**
     * Translates a POSIX pattern from a ~ operation into its corresponding Java regex pattern.
     *
     * @param posixPattern the POSIX pattern to translate
     * @param caseSensitive whether the translation should be case-sensitive
     * @return the translated Java regex pattern
     * @throws NullPointerException if the posixPattern is null
     * @throws IllegalArgumentException if case insensitive handling is not implemented yet
     *
     * Reference: https://www.postgresql.org/docs/9.0/functions-matching.html#FUNCTIONS-POSIX-REGEXP
     */
    public String translatePostgresPosix(String posixPattern, boolean caseSensitive) {
        Objects.requireNonNull(posixPattern);
        if (caseSensitive==false) {
            // TODO: add handling of insensitive case
            throw new IllegalArgumentException("Case insensitive handling not implemented yet");
        }
        return translateSimilarToPattern(posixPattern);
    }
}
