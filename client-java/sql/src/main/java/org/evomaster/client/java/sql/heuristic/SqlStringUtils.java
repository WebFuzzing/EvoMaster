package org.evomaster.client.java.sql.heuristic;

/**
 * Utility class for SQL strings.
 */
public class SqlStringUtils {
    private static final String SINGLE_QUOTE = "'";
    private static final String DOUBLE_QUOTE = "\"";

    /**
     * Removes enclosing single or double quotes from the input string.
     * If the input string starts and ends with either single or double quotes, it removes the first and last
     * characters of the string.
     *
     * @param input the input string
     * @return the string without enclosing quotes, or the original string if no enclosing quotes are found
     */
    public static String removeEnclosingQuotes(String input) {
        if (input == null || input.length() < 2) {
            return input;
        }
        if (startsAndEndsWith(input, SINGLE_QUOTE)
                || startsAndEndsWith(input, DOUBLE_QUOTE)) {
            return removeFirstAndLastChar(input);
        }
        return input;
    }

    private static String removeFirstAndLastChar(String input) {
        return input.substring(1, input.length() - 1);
    }

    private static boolean startsAndEndsWith(String input, String str) {
        return input.startsWith(str) && input.endsWith(str);
    }

 }
