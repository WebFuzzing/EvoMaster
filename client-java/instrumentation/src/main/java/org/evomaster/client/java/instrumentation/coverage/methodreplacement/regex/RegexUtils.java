package org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex;

public class RegexUtils {

    /**
     * Determine whether the regex requires features that are
     * not supported by the regex automaton library
     */
    public static boolean isSupportedRegex(String regex) {
        if (regex.contains("\\b"))
            return false;

        return true;
    }

    /**
     * Java regular expressions contain predefined character classes which the
     * regex parser cannot handle
     *
     * @param regex
     * @return
     */
    public static String expandRegex(String regex) {

        /*
            FIXME: all these checks assume that replaced character is not already
            inside a []
         */

        // .	Any character (may or may not match line terminators)
        // \d	A digit: [0-9]
        String newRegex = regex.replaceAll("\\\\d", "[0-9]");

        // \D	A non-digit: [^0-9]
        newRegex = newRegex.replaceAll("\\\\D", "[^0-9]");

        // \s	A whitespace character: [ \t\n\x0B\f\r]
        //newRegex = newRegex.replaceAll("\\\\s", "[ \\t\\n\\f\\r]"); //FIXME does not work
        newRegex = newRegex.replaceAll("\\\\s", " ");

        // \S	A non-whitespace character: [^\s]
        //newRegex = newRegex.replaceAll("\\\\S", "[^ \\t\\n\\f\\r]"); //FIXME does not work
        newRegex = newRegex.replaceAll("\\\\S", "[a-zA-Z_0-9]");

        // \w	A word character: [a-zA-Z_0-9]
        newRegex = newRegex.replaceAll("\\\\w", "[a-zA-Z_0-9]");

        // \W	A non-word character: [^\w]
        newRegex = newRegex.replaceAll("\\\\W", "[^a-zA-Z_0-9]");

        if (newRegex.startsWith("^"))
            newRegex = newRegex.substring(1);

        if (newRegex.endsWith("$"))
            newRegex = newRegex.substring(0, newRegex.length() - 1);

        // TODO: Some of these should be handled, not just ignored!
        newRegex = removeFlagExpressions(newRegex);

        newRegex = removeReluctantOperators(newRegex);

        return newRegex;
    }

    private static String removeFlagExpressions(String regex) {
        // Case insensitive
        regex = regex.replaceAll("\\(\\?i\\)", "");

        // Unix lines mode
        regex = regex.replaceAll("\\(\\?d\\)", "");

        // Permit comments and whitespace in pattern
        regex = regex.replaceAll("\\(\\?x\\)", "");

        // Multiline mode
        regex = regex.replaceAll("\\(\\?m\\)", "");

        // Dotall
        regex = regex.replaceAll("\\(\\?s\\)", "");

        // Unicode case
        regex = regex.replaceAll("\\(\\?u\\)", "");

        return regex;
    }

    private static String removeReluctantOperators(String regex) {
        regex = regex.replaceAll("\\+\\?", "\\+");
        regex = regex.replaceAll("\\*\\?", "\\*");
        regex = regex.replaceAll("\\?\\?", "\\?");

        return regex;
    }
}
