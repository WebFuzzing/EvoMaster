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

    /**
     * Compares two strings for equality, ignoring case considerations, and safely handles null values.
     *
     * @param a the first string to compare, may be null
     * @param b the second string to compare, may be null
     * @return {@code true} if both strings are equal ignoring case, or both are null;
     * {@code false} otherwise
     */
    public static boolean nullSafeEqualsIgnoreCase(String a, String b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equalsIgnoreCase(b);
        }
    }

    /**
     * Checks if a string ends with another string, ignoring case considerations.
     * It safely handles null values.
     *
     * @param a
     * @param suffix
     * @return
     */
    public static boolean nullSafeEndsWithIgnoreCase(String a, String suffix) {
        if (a == null) {
            return false;
        } else if (suffix == null) {
            return false;
        } else {
            return a.toLowerCase().endsWith(suffix.toLowerCase());
        }
    }
}
