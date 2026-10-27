package org.evomaster.client.java.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Reads and writes the one field of a JSON payload that a correlation id travels in, located
 * by a JSON Pointer (RFC 6901).
 *
 * A small parser rather than Jackson: the controller jar shades Jackson, so using it here would
 * ask every driver for an unshaded jackson-databind. Numbers, booleans and nulls are kept as
 * written, so a payload goes out as the core generated it, apart from whitespace and the field
 * written into it.
 */
final class JsonPointers {

    private static final char POINTER_SEPARATOR = '/';

    private static final String ESCAPED_SLASH = "~1";

    private static final String ESCAPED_TILDE = "~0";

    private static final String SLASH = "/";

    private static final String TILDE = "~";

    private static final String JSON_NULL = "null";

    private static final String JSON_TRUE = "true";

    private static final String JSON_FALSE = "false";

    private static final int HEX_RADIX = 16;

    /**
     * An array index as RFC 6901 writes one: 0, or digits not starting with 0.
     */
    private static final Pattern ARRAY_INDEX = Pattern.compile("0|[1-9][0-9]*");

    private static final int UNICODE_ESCAPE_DIGITS = 4;

    /**
     * Below this, a character has to be escaped inside a JSON string.
     */
    private static final char FIRST_UNESCAPED_CHAR = 0x20;

    private JsonPointers() {
    }

    /**
     * A number, true, false or null, kept exactly as written.
     */
    private static final class Literal {

        private final String text;

        private Literal(String text) {
            this.text = text;
        }
    }

    /**
     * The reference tokens of a pointer, unescaped, as RFC 6901 reads them: {@code "/a~1b/c"}
     * gives {@code a/b} and {@code c}, {@code "/"} gives the one empty token (the field named
     * {@code ""}), and a null or empty pointer, which is the whole document, gives none.
     *
     * @return null when the pointer is not one: not empty, and not starting with {@code /}
     */
    static List<String> tokensOf(String pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return Collections.emptyList();
        }
        if (pointer.charAt(0) != POINTER_SEPARATOR) {
            return null;
        }
        List<String> tokens = new ArrayList<>();
        //-1 keeps trailing empty tokens: "/a/" names the field "" inside "a"
        String[] parts = pointer.substring(1).split(SLASH, -1);
        for (String part : parts) {
            tokens.add(unescape(part));
        }
        return tokens;
    }

    /**
     * A single non-blank header field, with JSON Pointer escaping decoded, or null when
     * the pointer is missing, invalid, or addresses a nested field. Matches the schema parser.
     */
    static String headerFieldName(String pointer) {
        List<String> tokens = tokensOf(pointer);
        return tokens != null && tokens.size() == 1 && !tokens.get(0).trim().isEmpty()
                ? tokens.get(0) : null;
    }

    private static String unescape(String token) {
        //in this order, so that "~01" becomes "~1" and not "/"
        return token.replace(ESCAPED_SLASH, SLASH).replace(ESCAPED_TILDE, TILDE);
    }

    /**
     * The value at {@code pointer} in {@code json}, as text: a string without its quotes, a
     * number or boolean as written.
     *
     * @return null when the payload is not JSON, the pointer is not one, nothing is at the
     * pointer, or what is there is an object, an array or a JSON null
     */
    static String read(String json, String pointer) {
        List<String> tokens = tokensOf(pointer);
        if (json == null || tokens == null) {
            return null;
        }
        Object node;
        try {
            node = new Parser(json).parseDocument();
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (String token : tokens) {
            node = child(node, token);
            if (node == null) {
                return null;
            }
        }
        if (node instanceof String) {
            return (String) node;
        }
        if (node instanceof Literal && !JSON_NULL.equals(((Literal) node).text)) {
            return ((Literal) node).text;
        }
        return null;
    }

    /**
     * What {@code token} names in {@code node}: a field of an object, or an element of an
     * array, or null when there is none.
     */
    private static Object child(Object node, String token) {
        if (node instanceof Map) {
            return ((Map<?, ?>) node).get(token);
        }
        if (node instanceof List) {
            List<?> list = (List<?>) node;
            int index = indexOf(token, list.size());
            return index < 0 ? null : list.get(index);
        }
        return null;
    }

    /**
     * The array index {@code token} names, as RFC 6901 writes one: digits with no sign and no
     * leading zero.
     *
     * @return -1 when it is not an index, or not one below {@code size}
     */
    private static int indexOf(String token, int size) {
        if (!ARRAY_INDEX.matcher(token).matches()) {
            return -1;
        }
        try {
            int index = Integer.parseInt(token);
            return index < size ? index : -1;
        } catch (NumberFormatException e) {
            //too large for an int, so beyond any array
            return -1;
        }
    }

    /**
     * {@code json} with the string {@code value} written at {@code pointer}, replacing what was
     * there. Fields missing along the way are added as objects. An array along the way is
     * entered by index, as {@link #read} does, and an element is only ever replaced, never
     * added.
     *
     * @return the payload unchanged when it is not a JSON object, the pointer is not one or
     * names the whole document, or the pointer goes through something that cannot hold what it
     * names: a string, number, boolean or JSON null, an index past the end of an array, or a
     * token that is no index inside one. Nothing is overwritten to make room
     */
    static String write(String json, String pointer, String value) {
        Objects.requireNonNull(value);
        List<String> tokens = tokensOf(pointer);
        if (json == null || tokens == null || tokens.isEmpty()) {
            return json;
        }
        Object root;
        try {
            root = new Parser(json).parseDocument();
        } catch (IllegalArgumentException e) {
            return json;
        }
        if (!(root instanceof Map)) {
            return json;
        }

        Object holder = root;
        for (String token : tokens.subList(0, tokens.size() - 1)) {
            Object next = child(holder, token);
            //a field that is there holds something, a JSON null included, so null means missing
            if (next == null && holder instanceof Map) {
                next = new LinkedHashMap<String, Object>();
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) holder;
                fields.put(token, next);
            }
            if (!(next instanceof Map) && !(next instanceof List)) {
                return json;
            }
            holder = next;
        }

        String last = tokens.get(tokens.size() - 1);
        if (holder instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) holder;
            fields.put(last, value);
        } else {
            @SuppressWarnings("unchecked")
            List<Object> elements = (List<Object>) holder;
            int index = indexOf(last, elements.size());
            if (index < 0) {
                return json;
            }
            elements.set(index, value);
        }

        StringBuilder out = new StringBuilder(json.length() + value.length());
        serialize(root, out);
        return out.toString();
    }

    private static void serialize(Object node, StringBuilder out) {
        if (node instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                quote((String) entry.getKey(), out);
                out.append(':');
                serialize(entry.getValue(), out);
            }
            out.append('}');
        } else if (node instanceof List) {
            out.append('[');
            boolean first = true;
            for (Object element : (List<?>) node) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                serialize(element, out);
            }
            out.append(']');
        } else if (node instanceof String) {
            quote((String) node, out);
        } else {
            out.append(((Literal) node).text);
        }
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c < FIRST_UNESCAPED_CHAR) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    /**
     * A recursive-descent parser into {@link LinkedHashMap} (objects, keeping their field
     * order), {@link ArrayList} (arrays), {@link String} and {@link Literal}. Throws
     * {@link IllegalArgumentException} on anything that is not JSON.
     */
    private static final class Parser {

        private final String text;

        private int pos = 0;

        private Parser(String text) {
            this.text = text;
        }

        Object parseDocument() {
            Object value = parseValue();
            skipWhitespace();
            if (pos != text.length()) {
                throw error("trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= text.length()) {
                throw error("unexpected end");
            }
            char c = text.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                default:
                    return parseLiteral();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return object;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("expected a field name");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return object;
                }
                if (c != ',') {
                    throw error("expected ',' or '}'");
                }
            }
        }

        private List<Object> parseArray() {
            List<Object> array = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return array;
                }
                if (c != ',') {
                    throw error("expected ',' or ']'");
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return value.toString();
                }
                if (c != '\\') {
                    value.append(c);
                    continue;
                }
                char escaped = next();
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(escaped);
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'u':
                        if (pos + UNICODE_ESCAPE_DIGITS > text.length()) {
                            throw error("truncated unicode escape");
                        }
                        try {
                            value.append((char) Integer.parseInt(
                                    text.substring(pos, pos + UNICODE_ESCAPE_DIGITS), HEX_RADIX));
                        } catch (NumberFormatException e) {
                            throw error("bad unicode escape");
                        }
                        pos += UNICODE_ESCAPE_DIGITS;
                        break;
                    default:
                        throw error("bad escape");
                }
            }
        }

        /**
         * A number, true, false or null. Only checked to be made of the characters those can
         * contain, as it is written back exactly as read.
         */
        private Literal parseLiteral() {
            int start = pos;
            while (pos < text.length() && isLiteralChar(text.charAt(pos))) {
                pos++;
            }
            if (start == pos) {
                throw error("unexpected character");
            }
            String literal = text.substring(start, pos);
            char first = literal.charAt(0);
            boolean number = first == '-' || Character.isDigit(first);
            if (!number && !literal.equals(JSON_TRUE) && !literal.equals(JSON_FALSE) && !literal.equals(JSON_NULL)) {
                throw error("unknown literal " + literal);
            }
            return new Literal(literal);
        }

        private boolean isLiteralChar(char c) {
            return Character.isLetterOrDigit(c) || c == '-' || c == '+' || c == '.';
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            if (pos >= text.length()) {
                throw error("unexpected end");
            }
            return text.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private void expect(char expected) {
            if (next() != expected) {
                throw error("expected '" + expected + "'");
            }
        }

        private IllegalArgumentException error(String what) {
            return new IllegalArgumentException("Not JSON, " + what + " at " + pos);
        }
    }
}
