package org.evomaster.client.java.controller.dynamodb;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Utilities to deal with DynamoDB SDK request/response values without
 * introducing direct compile-time dependencies to AWS SDK classes.
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

    private static final String DECIMAL_SEPARATOR = ".";
    private static final String SCIENTIFIC_NOTATION_E_LOWER = "e";
    private static final String SCIENTIFIC_NOTATION_E_UPPER = "E";

    /**
     * Utility class, no instances.
     */
    private DynamoDbAttributeValueHelper() {
    }

    /**
     * Converts a map of DynamoDB attribute values into plain Java values.
     *
     * @param source input object expected to be a map
     * @return normalized map or empty map when input is not a map
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
     * Converts one DynamoDB attribute value object into a plain Java value.
     *
     * @param value attribute value object
     * @return normalized Java value
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
     * Converts binary payloads into plain byte arrays when backed by ByteBuffer.
     *
     * @param value binary payload object
     * @return byte array or original value when conversion is not needed
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
     * Reads a reflected value only when its corresponding {@code hasX} accessor is true.
     *
     * @param target target object
     * @param hasMethod presence-check method name
     * @param valueMethod value accessor method name
     * @return reflected value or {@code null}
     */
    private static Object readIfPresent(Object target, String hasMethod, String valueMethod) {
        if (Boolean.TRUE.equals(DynamoDbReflectionHelper.invokeBooleanNoArg(target, hasMethod))) {
            return DynamoDbReflectionHelper.invokeNoArg(target, valueMethod);
        }
        return null;
    }

    /**
     * Converts a collection of attribute values into plain Java values.
     *
     * @param source source collection
     * @return normalized list
     */
    private static List<Object> toPlainList(Collection<Object> source) {
        List<Object> converted = new ArrayList<>(source.size());
        for (Object element : source) {
            converted.add(toPlainValue(element));
        }
        return converted;
    }

    /**
     * Converts a collection of numeric tokens into parsed numeric values.
     *
     * @param source source numeric collection
     * @return normalized number set
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
     * Converts a collection of binary payloads into plain binary values.
     *
     * @param source source binary collection
     * @return normalized binary set
     */
    private static Set<Object> toBinarySet(Collection<?> source) {
        LinkedHashSet<Object> binaries = new LinkedHashSet<>();
        for (Object binary : source) {
            binaries.add(toPlainBinary(binary));
        }
        return binaries;
    }

    /**
     * Parses a numeric token into {@link Long} or {@link Double}.
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
}
