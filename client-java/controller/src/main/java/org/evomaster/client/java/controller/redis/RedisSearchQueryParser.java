package org.evomaster.client.java.controller.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parses the subset of the RediSearch query language supported by the heuristic calculation:
 *
 * <pre>
 * query         ::= "*" | filter (WS filter)*        // WS-separated filters = AND
 * filter        ::= tagFilter | numericFilter | textFilter
 * tagFilter     ::= "@" field ":{" value ("|" value)* "}"
 * numericFilter ::= "@" field ":[" number WS number "]"
 * textFilter    ::= ["@" field ":"] term
 * term          ::= word | word "*"
 * </pre>
 *
 * Boolean OR/NOT between filters, open-ended and exclusive numeric ranges, GEO/VECTOR filters,
 * and JSON-backed indexes are not part of this grammar. A query using any of them will either
 * fail to parse (raising {@link IllegalArgumentException}, handled by the caller as an unknown
 * condition) or be parsed as a literal text term, since neither can be told apart syntactically
 * from a plain word.
 */
public class RedisSearchQueryParser {

    private RedisSearchQueryParser() {
    }

    public static List<RedisSearchFilter> parse(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Redis search query must not be null");
        }

        String trimmed = query.trim();
        if (trimmed.equals("*")) {
            return Collections.emptyList();
        }

        List<RedisSearchFilter> filters = new ArrayList<>();
        for (String token : tokenize(trimmed)) {
            filters.add(parseFilter(token));
        }
        return filters;
    }

    /**
     * Splits a query into its top-level filter tokens on whitespace, without breaking apart a
     * numericFilter's internal "min WS max" separator, by tracking bracket/brace depth.
     */
    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);

            if (c == '[' || c == '{') {
                depth++;
            } else if (c == ']' || c == '}') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("Unbalanced brackets in Redis search query: " + query);
                }
            }

            if (Character.isWhitespace(c) && depth == 0) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (depth != 0) {
            throw new IllegalArgumentException("Unbalanced brackets in Redis search query: " + query);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static RedisSearchFilter parseFilter(String token) {
        if (!token.startsWith("@")) {
            return new RedisSearchTextFilter(null, token);
        }

        int colon = token.indexOf(':');
        if (colon < 0) {
            return new RedisSearchTextFilter(null, token);
        }

        String field = token.substring(1, colon);
        String rest = token.substring(colon + 1);

        if (rest.startsWith("{") && rest.endsWith("}")) {
            String inner = rest.substring(1, rest.length() - 1);
            List<String> values = Arrays.asList(inner.split("\\|", -1));
            return new RedisSearchTagFilter(field, values);
        }

        if (rest.startsWith("[") && rest.endsWith("]")) {
            String inner = rest.substring(1, rest.length() - 1).trim();
            String[] bounds = inner.split("\\s+");
            if (bounds.length != 2) {
                throw new IllegalArgumentException("Malformed numeric range in Redis search query: " + token);
            }
            double min = Double.parseDouble(bounds[0]);
            double max = Double.parseDouble(bounds[1]);
            return new RedisSearchNumericFilter(field, min, max);
        }

        return new RedisSearchTextFilter(field, rest);
    }
}
