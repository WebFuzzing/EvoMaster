package org.evomaster.client.java.controller.redis;

/**
 * Utils class for auxiliary operations in Redis heuristic value calculations.
 */
public class RedisUtils {

    /**
     * Translates a Redis glob-style pattern into a valid Java regex pattern.
     * Supported glob-style patterns:
     *
     * h?llo matches hello, hallo and hxllo
     * h*llo matches hllo and heeeello
     * h[ae]llo matches hello and hallo, but not hillo
     * h[^e]llo matches hallo, hbllo, ... but not hello
     * h[a-b]llo matches hallo and hbllo
     * Use \ to escape special characters if you want to match them verbatim.
     *
     * Supported conversions:
     * - *  →  .*
     * - ?  →  .
     * - [ae]  →  [ae]
     * - [^e]  →  [^e]
     * - [a-b]  →  [a-b]
     * Other regex metacharacters are properly escaped.
     *
     * @param redisPattern the Redis glob-style pattern (e.g., "h?llo*", "user:[0-9]*")
     * @return a valid Java regex string equivalent to the Redis pattern.
     */
    public static String redisPatternToRegex(String redisPattern) {
        if (redisPattern == null || redisPattern.isEmpty()) {
            return ".*";
        }

        StringBuilder regex = new StringBuilder();
        boolean inBrackets = false;
        boolean escaping = false;

        for (int i = 0; i < redisPattern.length(); i++) {
            char c = redisPattern.charAt(i);

            if (escaping) {
                if (".+(){}|^$[]\\".indexOf(c) >= 0) {
                    regex.append('\\');
                }
                regex.append(c);
                escaping = false;
                continue;
            }

            switch (c) {
                case '\\':
                    escaping = true;
                    break;

                case '*':
                    regex.append(".*");
                    break;

                case '?':
                    regex.append('.');
                    break;

                case '[':
                    if (inBrackets) {
                        throw new IllegalArgumentException("Malformed Redis pattern: nested [");
                    }
                    inBrackets = true;
                    regex.append('[');
                    if (i + 1 < redisPattern.length() && redisPattern.charAt(i + 1) == '^') {
                        regex.append('^');
                        i++;
                    }
                    break;

                case ']':
                    if (inBrackets) {
                        regex.append(']');
                        inBrackets = false;
                    } else {
                        regex.append("\\]");
                    }
                    break;

                default:
                    if (!inBrackets && ".+(){}|^$".indexOf(c) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(c);
                    break;
            }
        }

        if (escaping) {
            throw new IllegalArgumentException("Malformed Redis pattern: trailing backslash");
        }

        if (inBrackets) {
            throw new IllegalArgumentException("Malformed Redis pattern: unclosed [");
        }

        return "^" + regex + "$";
    }
}
