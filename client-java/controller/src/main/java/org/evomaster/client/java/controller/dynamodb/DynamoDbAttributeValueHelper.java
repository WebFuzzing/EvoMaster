package org.evomaster.client.java.controller.dynamodb;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Normalizes DynamoDB request and response values into plain Java objects used by the expression
 * parser and heuristic calculator.
 * <p>
 * DynamoDB SDK versions expose attribute values through similar accessor methods but different
 * concrete classes. This helper calls those accessors through {@link DynamoDbReflectionHelper} so
 * the controller does not need a compile-time dependency on a particular AWS SDK. The resulting
 * representation uses maps, lists, sets, strings, numbers, booleans, and binary values that can be
 * inspected without SDK-specific logic.
 * <p>
 * The class also resolves DynamoDB attribute types and traverses the restricted document-path
 * syntax accepted by {@code DynamoDbConditionExpression.g4} directly over the normalized maps and
 * lists. Direct traversal preserves the original Java values and distinguishes a missing path from
 * a path whose value is explicitly {@code null}.
 */
public final class DynamoDbAttributeValueHelper {

    /**
     * Reflection-bound AWS AttributeValue accessors. Keep these literals unchanged:
     * they must match SDK method names exactly.
     */
    private static final String METHOD_NUL = "nul";
    private static final String METHOD_S = "s";
    private static final String METHOD_N = "n";
    private static final String METHOD_BOOL = "bool";
    private static final String METHOD_HAS_M = "hasM";
    private static final String METHOD_M = "m";
    private static final String METHOD_HAS_L = "hasL";
    private static final String METHOD_L = "l";
    private static final String METHOD_HAS_SS = "hasSs";
    private static final String METHOD_SS = "ss";
    private static final String METHOD_HAS_NS = "hasNs";
    private static final String METHOD_NS = "ns";
    private static final String METHOD_HAS_BS = "hasBs";
    private static final String METHOD_BS = "bs";
    private static final String METHOD_B = "b";

    //Constants for value parsing
    private static final String DECIMAL_SEPARATOR = ".";
    private static final String SCIENTIFIC_NOTATION_E_LOWER = "e";
    private static final String SCIENTIFIC_NOTATION_E_UPPER = "E";

    //Constants for query results parsing
    private static final String PATH_DOT_SEPARATOR_REGEX = "\\.";
    private static final char LIST_INDEX_OPEN = '[';
    private static final char LIST_INDEX_CLOSE = ']';


    /**
     * Prevents instantiation because this class contains only stateless conversion and lookup
     * operations.
     */
    private DynamoDbAttributeValueHelper() {
    }

    /**
     * Converts a map of DynamoDB attribute values into a deterministic map of plain Java values.
     * <p>
     * Keys are converted to strings and null keys are omitted because DynamoDB attribute names are
     * strings. Values are recursively normalized with {@link #toPlainValue(Object)}. A
     * {@link LinkedHashMap} is used so source iteration order is retained for reproducible heuristic
     * evaluation and tests.
     *
     * @param source input object expected to be a map
     * @return normalized map, or an empty map when {@code source} is not a map
     */
    public static Map<String, Object> toPlainMap(Object source) {
        if (!(source instanceof Map<?, ?>)) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        ((Map<?, ?>) source).forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), toPlainValue(value));
            }
        });
        return result;
    }

    /**
     * Converts one DynamoDB attribute value object into the corresponding plain Java value.
     * <p>
     * Already-plain maps and collections are recursively normalized first. Other objects are
     * inspected through the accessor names shared by DynamoDB SDK attribute-value implementations.
     * Scalar values are checked before document and set values to match the mutually exclusive
     * DynamoDB attribute model. Unknown objects are returned unchanged so callers can still handle
     * SDK variants or test values that this helper does not recognize.
     *
     * @param value attribute value object
     * @return recursively normalized Java value, or the original object when no known DynamoDB
     * attribute shape is available
     */
    @SuppressWarnings("unchecked")
    public static Object toPlainValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?>) {
            return toPlainMap(value);
        }

        if (value instanceof Collection<?>) {
            return toPlainList((Collection<Object>) value);
        }

        // The AWS SDK AttributeValue class exposes "hasXxx"/"xxx" methods.
        // We use reflection to stay decoupled from specific SDK versions.
        Object nul = DynamoDbReflectionHelper.invokeBooleanNoArg(value, METHOD_NUL);
        if (Boolean.TRUE.equals(nul)) {
            return null;
        }

        Object s = DynamoDbReflectionHelper.invokeNoArg(value, METHOD_S);
        if (s instanceof String) {
            return s;
        }

        Object n = DynamoDbReflectionHelper.invokeNoArg(value, METHOD_N);
        if (n instanceof String && !((String) n).isEmpty()) {
            return parseNumber((String) n);
        }

        Object bool = DynamoDbReflectionHelper.invokeNoArg(value, METHOD_BOOL);
        if (bool instanceof Boolean) {
            return bool;
        }

        Object m = readIfPresent(value, METHOD_HAS_M, METHOD_M);
        if (m instanceof Map<?, ?>) {
            return toPlainMap(m);
        }

        Object l = readIfPresent(value, METHOD_HAS_L, METHOD_L);
        if (l instanceof Collection<?>) {
            return toPlainList((Collection<Object>) l);
        }

        Object ss = readIfPresent(value, METHOD_HAS_SS, METHOD_SS);
        if (ss instanceof Collection<?>) {
            return new LinkedHashSet<>((Collection<?>) ss);
        }

        Object ns = readIfPresent(value, METHOD_HAS_NS, METHOD_NS);
        if (ns instanceof Collection<?>) {
            return toNumberSet((Collection<?>) ns);
        }

        Object bs = readIfPresent(value, METHOD_HAS_BS, METHOD_BS);
        if (bs instanceof Collection<?>) {
            return toBinarySet((Collection<?>) bs);
        }

        Object b = DynamoDbReflectionHelper.invokeNoArg(value, METHOD_B);
        if (b != null) {
            return toPlainBinary(b);
        }

        return value;
    }

    /**
     * Resolves the DynamoDB attribute type represented by a normalized Java runtime value.
     * <p>
     * The heuristic calculator uses this mapping to evaluate {@code attribute_type}. Sets are
     * classified by a representative element because Java generic element types are erased. Empty
     * and unrecognized sets fall back to {@link DynamoDbAttributeType#LIST}, while unsupported
     * scalar objects fall back to {@link DynamoDbAttributeType#STRING}; these defaults preserve the
     * existing behavior for values without enough runtime type information.
     *
     * @param value runtime value
     * @return DynamoDB attribute type used by type predicates
     */
    public static DynamoDbAttributeType resolveAttributeType(Object value) {
        if (value == null) {
            return DynamoDbAttributeType.NULL;
        }
        if (value instanceof String) {
            return DynamoDbAttributeType.STRING;
        }
        if (value instanceof Number) {
            return DynamoDbAttributeType.NUMBER;
        }
        if (value instanceof byte[]) {
            return DynamoDbAttributeType.BINARY;
        }
        if (value instanceof Boolean) {
            return DynamoDbAttributeType.BOOLEAN;
        }
        if (value instanceof Map<?, ?>) {
            return DynamoDbAttributeType.MAP;
        }
        if (value instanceof Set<?>) {
            Set<?> set = (Set<?>) value;
            if (set.isEmpty()) {
                return DynamoDbAttributeType.LIST;
            }
            Object sample = set.iterator().next();
            if (sample instanceof String) {
                return DynamoDbAttributeType.STRING_SET;
            }
            if (sample instanceof Number) {
                return DynamoDbAttributeType.NUMBER_SET;
            }
            if (sample instanceof byte[]) {
                return DynamoDbAttributeType.BINARY_SET;
            }
            return DynamoDbAttributeType.LIST;
        }
        if (value instanceof Collection<?>) {
            return DynamoDbAttributeType.LIST;
        }
        return DynamoDbAttributeType.STRING;
    }

    /**
     * Converts a {@link ByteBuffer}-backed binary payload into an independent byte array.
     * <p>
     * A read-only duplicate is consumed so conversion does not modify the position of the buffer
     * owned by the SDK or caller. Other binary wrappers are returned unchanged because accessing
     * their bytes would require an SDK-specific dependency.
     *
     * @param value binary payload object
     * @return byte array for a {@code ByteBuffer}, or the original value for other representations
     */
    private static Object toPlainBinary(Object value) {
        if (value instanceof ByteBuffer) {
            ByteBuffer bb = ((ByteBuffer) value).asReadOnlyBuffer();
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            return bytes;
        }

        return value;
    }

    /**
     * Reads a reflected collection or document value only when its corresponding presence accessor
     * reports that the attribute shape is set.
     * <p>
     * DynamoDB SDK accessors can return empty default collections even when a field was not
     * populated. Consulting {@code hasX} first prevents an unset map, list, or set from being
     * mistaken for the active value of the attribute.
     *
     * @param target target object
     * @param hasMethod presence-check method name
     * @param valueMethod value accessor method name
     * @return reflected value when present, otherwise {@code null}
     */
    private static Object readIfPresent(Object target, String hasMethod, String valueMethod) {
        if (Boolean.TRUE.equals(DynamoDbReflectionHelper.invokeBooleanNoArg(target, hasMethod))) {
            return DynamoDbReflectionHelper.invokeNoArg(target, valueMethod);
        }
        return null;
    }

    /**
     * Recursively converts a DynamoDB list into an ordered list of plain Java values.
     * <p>
     * List order is semantically significant for document-path indexes and must therefore be
     * preserved during normalization.
     *
     * @param source source collection
     * @return normalized list in source iteration order
     */
    private static List<Object> toPlainList(Collection<Object> source) {
        List<Object> converted = new ArrayList<>(source.size());
        for (Object element : source) {
            converted.add(toPlainValue(element));
        }
        return converted;
    }

    /**
     * Converts a DynamoDB number set into parsed Java numeric values.
     * <p>
     * A {@link LinkedHashSet} removes duplicate values while keeping stable source order. Null
     * elements are skipped because they cannot represent members of a DynamoDB number set.
     *
     * @param source source numeric collection
     * @return normalized, deterministic number set
     */
    private static Set<Object> toNumberSet(Collection<?> source) {
        LinkedHashSet<Object> numbers = new LinkedHashSet<>();
        for (Object number : source) {
            if (number != null) {
                numbers.add(parseNumber(String.valueOf(number)));
            }
        }
        return numbers;
    }

    /**
     * Converts every member of a DynamoDB binary set into its plain binary representation.
     * <p>
     * The deterministic set retains source iteration order and delegates individual buffer handling
     * to {@link #toPlainBinary(Object)}.
     *
     * @param source source binary collection
     * @return normalized, deterministic binary set
     */
    private static Set<Object> toBinarySet(Collection<?> source) {
        LinkedHashSet<Object> binaries = new LinkedHashSet<>();
        for (Object binary : source) {
            binaries.add(toPlainBinary(binary));
        }
        return binaries;
    }

    /**
     * Parses a DynamoDB numeric token into the narrow plain representation used by heuristics.
     * <p>
     * Integral tokens become {@link Long}; decimal and scientific-notation tokens become
     * {@link Double}. Returning {@link Double#NaN} for malformed input keeps conversion total and
     * lets later comparison logic treat the value as non-finite instead of failing request parsing.
     *
     * @param text numeric token
     * @return parsed number or {@link Double#NaN} when parsing fails
     */
    private static Object parseNumber(String text) {
        try {
            if (text.contains(DECIMAL_SEPARATOR)
                    || text.contains(SCIENTIFIC_NOTATION_E_LOWER)
                    || text.contains(SCIENTIFIC_NOTATION_E_UPPER)) {
                return Double.parseDouble(text);
            }
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Returns every resolvable document path in a normalized DynamoDB item.
     * <p>
     * Map fields use dot notation and list elements use bracketed indexes, matching
     * {@link #lookupByPath(Map, String)}. Container paths are included alongside their descendants,
     * while sets are treated as leaves because DynamoDB document paths cannot address set members.
     * Paths retain the deterministic traversal order of the normalized maps and lists.
     * <p>
     * Children are pushed onto a LIFO work stack in reverse order to preserve depth-first preorder.
     *
     * @param item normalized DynamoDB item
     * @return snapshot of all resolvable document paths, or an empty set when {@code item} is null
     */
    public static Set<String> documentPaths(Map<String, Object> item) {
        Set<String> paths = new LinkedHashSet<>();
        if (item == null) {
            return paths;
        }

        Deque<DocumentPathNode> pending = new ArrayDeque<>();
        pending.push(new DocumentPathNode(item, null)); //Use null as parent for the root node

        while (!pending.isEmpty()) {
            DocumentPathNode current = pending.pop();
            if (current.path != null) {
                paths.add(current.path);
            }

            if (current.value instanceof Map<?, ?>) {
                List<Map.Entry<?, ?>> entries = new ArrayList<>(((Map<?, ?>) current.value).entrySet());
                for (int i = entries.size() - 1; i >= 0; i--) {
                    Map.Entry<?, ?> entry = entries.get(i);
                    Objects.requireNonNull(entry.getKey());

                    String field = String.valueOf(entry.getKey());
                    String path = current.path == null ? field : current.path + "." + field;
                    pending.push(new DocumentPathNode(entry.getValue(), path));
                }
                continue;
            }

            if (current.value instanceof List<?>) {
                List<?> list = (List<?>) current.value;
                for (int i = list.size() - 1; i >= 0; i--) {
                    String path = current.path + LIST_INDEX_OPEN + i + LIST_INDEX_CLOSE;
                    pending.push(new DocumentPathNode(list.get(i), path));
                }
            }
        }
        return paths;
    }

    /**
     * Resolves a DynamoDB document path against a normalized item without converting it to JSON.
     * <p>
     * Supported paths contain dot-separated map fields and zero or more list indexes on each field,
     * for example {@code profile.country}, {@code squads[0].captain}, or
     * {@code tournaments[0][1]}. Direct map/list traversal avoids an additional JSON dependency or
     * tree conversion and preserves the runtime types consumed by the heuristic calculator.
     * <p>
     * Lookup fails when the item or path is absent, a field does not exist, an intermediate value has
     * the wrong container type, or a list index is outside the available range. A successfully
     * resolved explicit null is returned as {@code DynamoDbValueLookup(true, null)}.
     *
     * @param item normalized DynamoDB item
     * @param path document path produced by the DynamoDB expression parser
     * @return result containing both path-presence information and the resolved value
     */
    public static DynamoDbValueLookup lookupByPath(Map<String, Object> item, String path) {
        if (item == null || path == null || path.trim().isEmpty()) {
            return new DynamoDbValueLookup(false, null);
        }

        Object current = item;
        String[] chunks = path.split(PATH_DOT_SEPARATOR_REGEX);
        for (String rawChunk : chunks) {
            ParsedChunk chunk = parseChunk(rawChunk);

            if (!(current instanceof Map<?, ?>)) {
                return new DynamoDbValueLookup(false, null);
            }

            Map<?, ?> map = (Map<?, ?>) current;
            if (!map.containsKey(chunk.fieldName)) {
                return new DynamoDbValueLookup(false, null);
            }
            current = map.get(chunk.fieldName);

            for (Integer index : chunk.indexes) {
                if (!(current instanceof List<?>)) {
                    return new DynamoDbValueLookup(false, null);
                }
                List<?> list = (List<?>) current;
                if (index < 0 || index >= list.size()) {
                    return new DynamoDbValueLookup(false, null);
                }
                current = list.get(index);
            }
        }

        return new DynamoDbValueLookup(true, current);
    }

    /**
     * Separates one dot-delimited path chunk into its map field and ordered list indexes.
     * <p>
     * Normal calls receive chunks validated by the expression grammar, such as {@code squads[0]}.
     * For compatibility with the existing permissive behavior, malformed or incomplete index
     * literals are ignored rather than reported as errors.
     * <p>
     * For example, {@code tournaments[2][1]} is parsed as field name {@code tournaments} with
     * indexes {@code [2, 1]}. Path lookup reads the map field first, then applies index {@code 2}
     * followed by index {@code 1} to the resulting nested lists.
     *
     * @param chunk one field-and-index component of a DynamoDB document path
     * @return parsed field name and indexes to traverse
     */
    private static ParsedChunk parseChunk(String chunk) {
        Objects.requireNonNull(chunk);
        String field = chunk;
        List<Integer> indexes = new ArrayList<>();

        int bracketStart = chunk.indexOf(LIST_INDEX_OPEN);
        if (bracketStart >= 0) {
            field = chunk.substring(0, bracketStart);
            int cursor = bracketStart;
            while (cursor < chunk.length()) {
                int start = chunk.indexOf(LIST_INDEX_OPEN, cursor);
                if (start < 0) {
                    break;
                }
                int end = chunk.indexOf(LIST_INDEX_CLOSE, start);
                if (end < 0) {
                    break;
                }
                String indexLiteral = chunk.substring(start + 1, end).trim();
                try {
                    indexes.add(Integer.parseInt(indexLiteral));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed indexes, path lookup will fail naturally.
                }
                cursor = end + 1;
            }
        }

        return new ParsedChunk(field, indexes);
    }

    /**
     * One pending value and its resolvable document path in the iterative traversal.
     */
    private static final class DocumentPathNode {
        private final Object value;
        private final String path;

        /**
         * Creates a pending traversal node.
         *
         * @param value normalized value to inspect
         * @param path path resolving the value, or null for the item root
         */
        private DocumentPathNode(Object value, String path) {
            this.value = value;
            this.path = path;
        }
    }

    /**
     * Internal representation of one document-path field and the list indexes applied after reading
     * that field.
     */
    private static final class ParsedChunk {
        private final String fieldName;
        private final List<Integer> indexes;

        /**
         * Creates a parsed document-path component.
         *
         * @param fieldName map field to resolve first
         * @param indexes list indexes to apply in encounter order
         */
        private ParsedChunk(String fieldName, List<Integer> indexes) {
            this.fieldName = fieldName;
            this.indexes = indexes;
        }
    }

}
