package com.webfuzzing.asyncapi.resolver;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code $ref} handling for AsyncAPI documents.
 *
 * There are two quite different jobs here, and they are treated differently on purpose.
 *
 * References <i>between AsyncAPI constructs</i> (a message pointing at a trait, a message
 * pointing at a correlation id) are resolved by {@link #resolveLocal} while parsing: the model
 * that comes out holds plain keys, never pointers.
 *
 * References <i>inside a JSON Schema</i> are not resolved at all, because whatever consumes
 * them later would rather resolve them itself. What is done instead is to make sure it can:
 * every schema a payload can reach has to sit in one flat map under
 * {@code #/components/schemas/}, which is what {@link #schemaKeyOf} is for.
 */
public class AsyncApiRefResolver {

    private static final String REF = "$ref";

    public static final String SCHEMA_PREFIX = "#/components/schemas/";

    private AsyncApiRefResolver() {
    }

    public static boolean isLocal(String ref) {
        return RefLocations.isLocalRef(ref);
    }

    /**
     * Follow a local {@code $ref} such as {@code #/components/messages/foo} inside
     * {@code root}. Returns null when the pointer does not lead anywhere.
     */
    public static JsonNode resolveLocal(JsonNode root, String ref) {

        if (!isLocal(ref)) {
            return null;
        }

        JsonNode current = root;

        for (String segment : ref.substring(1).split("/")) {

            if (segment.isEmpty()) {
                continue;
            }

            current = current.get(decodePointerSegment(segment));

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * The last segment of a {@code $ref}, i.e. the key of what it points at, but only if the
     * pointer has the shape we expect.
     * {@code refKey("#/components/messages/foo", "#/components/messages/")} gives "foo", while
     * a pointer somewhere else gives null.
     */
    public static String refKey(String ref, String expectedPrefix) {

        if (!ref.startsWith(expectedPrefix)) {
            return null;
        }

        String key = ref.substring(expectedPrefix.length());

        //must be a single segment: a deeper pointer is something else than what was asked for
        if (key.trim().isEmpty() || key.contains("/")) {
            return null;
        }

        return decodePointerSegment(key);
    }

    /**
     * The value of a node's {@code $ref}, or null when it has none.
     */
    public static String refOf(JsonNode node) {

        if (node == null) {
            return null;
        }

        JsonNode ref = node.get(REF);

        return ref != null && ref.isTextual() ? ref.asText() : null;
    }

    /**
     * The component schema a reference reaches into, or null if it addresses something else.
     *
     * Unlike {@link #refKey} this accepts a pointer that goes deeper than the schema itself,
     * such as {@code #/components/schemas/Order/properties/item}: the schema named by the first
     * segment is still what has to be present for the pointer to lead anywhere.
     */
    public static String schemaKeyOf(String ref) {

        if (!ref.startsWith(SCHEMA_PREFIX)) {
            return null;
        }

        String rest = ref.substring(SCHEMA_PREFIX.length());
        int slash = rest.indexOf('/');
        String key = slash < 0 ? rest : rest.substring(0, slash);

        return key.trim().isEmpty() ? null : decodePointerSegment(key);
    }

    /**
     * Every {@code $ref} value under {@code node}, at any depth.
     */
    public static List<String> collectRefs(JsonNode node) {
        List<String> refs = new ArrayList<>();
        collectRefsInto(node, refs);
        return refs;
    }

    private static void collectRefsInto(JsonNode node, List<String> out) {

        if (node.isObject()) {

            String ref = refOf(node);

            if (ref != null) {
                out.add(ref);
            }

            for (JsonNode value : node) {
                collectRefsInto(value, out);
            }

        } else if (node.isArray()) {
            for (JsonNode value : node) {
                collectRefsInto(value, out);
            }
        }
    }

    /**
     * JSON Pointer escaping: "~1" is a "/" and "~0" is a "~". Percent-encoding is undone too,
     * as references are URIs.
     */
    private static String decodePointerSegment(String segment) {

        String decoded = segment;

        //only when there is something to decode: URLDecoder would otherwise eat a literal '+'
        if (segment.indexOf('%') >= 0) {
            try {
                decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                decoded = segment;
            }
        }

        return decoded.replace("~1", "/").replace("~0", "~");
    }
}
