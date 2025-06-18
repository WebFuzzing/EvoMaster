package org.evomaster.client.java.instrumentation.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegexSharedUtils {


    public static String removeParentheses(String regex){

        String s = regex.trim();
        if(s.startsWith("(") && s.endsWith(")")){
           //would not work for "()()"
            // return removeParentheses(s.substring(1, s.length()-1));
        }
        return s;
    }

    /**
     * Make sure regex starts with ^ and ends with $
     * @param regex
     * @return
     */
    public static String forceFullMatch(String regex){

        String s = removeParentheses(regex);
        if(s.startsWith("^") && s.endsWith("$")){
            //nothing to do
            return s;
        }
        if(s.startsWith("^")){
            return s + "$";
        }
        if(s.endsWith("$")){
            return "^" + s;
        }

        return "^(" + s + ")$";
    }


    /**
     * Make sure that regex would match whole text even if originally would only match a subset
     * @param regex
     * @return
     */
    public static String handlePartialMatch(String regex){

        /*
            Bit tricky... (.*) before/after the regex would not work, as by default . does
            not match line terminators. enabling DOTALL flag is risky, as the original could
            use flags.
            \s\S is just a way to covering everything
         */

        String s = removeParentheses(regex);
        if(s.startsWith("^") && s.endsWith("$")){
            //nothing to do
            return s;
        }
        if(s.startsWith("^")){
            return s + "([\\s\\S]*)";
        }
        if(s.endsWith("$")){
            return "([\\s\\S]*)" + s;
        }

        return String.format("([\\s\\S]*)(%s)([\\s\\S]*)", regex);
    }

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
    public static String translateSqlLikePattern(String likePattern) {
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
    public static String translateSqlSimilarToPattern(String similarToPattern) {
        Objects.requireNonNull(similarToPattern);
        // TODO Translate SQL standard regular expression symbols into Java Regex symbols
        return translateSqlLikePattern(similarToPattern);
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
    public static String translatePostgresqlPosix(String posixPattern, boolean caseSensitive) {
        Objects.requireNonNull(posixPattern);
        if (caseSensitive==false) {
            // TODO: add handling of insensitive case
            throw new IllegalArgumentException("Case insensitive handling not implemented yet");
        }
        return translateSqlSimilarToPattern(posixPattern);
    }

}
