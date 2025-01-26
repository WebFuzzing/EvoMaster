package org.evomaster.client.java.sql.internal;

public class SqlStringUtils {
    private static final String QUOTE = "'";

    public static String removeEnclosingQuotes(String input) {
        if (input == null || input.length() < 2) {
            return input;
        }
        if (input.startsWith(QUOTE) && input.endsWith(QUOTE)) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }
}
