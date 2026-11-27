package org.evomaster.client.java.controller.api.dto.database.operations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Utility methods for creating and converting {@link DynamoDbAttributeValueDto} trees.
 * <p>
 * Supports scalars ({@code S}, {@code N}, {@code BOOL}), documents ({@code M}, {@code L}),
 * {@code NULL}, string sets ({@code SS}), and number sets ({@code NS}). Binary values ({@code B})
 * and binary sets ({@code BS}) are intentionally unsupported.
 */
public final class DynamoDbDtoUtils {

    private DynamoDbDtoUtils() {
    }

    /**
     * Copies normalized Java values into a typed tree.
     * Map keys and set elements are sorted for deterministic template selection; empty sets,
     * mixed-type sets, binary values ({@code B}), and binary sets ({@code BS}) are unsupported.
     *
     * @param name attribute name, or null for a list element
     * @param plain normalized scalar, map, list, non-empty string/number set, or null
     * @return independent typed value
     * @throws IllegalArgumentException if a map key is not a string, a set is empty/mixed/binary,
     *                                  or the value type is unsupported
     */
    public static DynamoDbAttributeValueDto fromPlainValue(String name, Object plain) {
        if (plain instanceof String) {
            return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.STRING, (String) plain);
        }
        if (plain instanceof Number) {
            return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.NUMBER, formatFiniteNumber((Number) plain));
        }
        if (plain instanceof Boolean) {
            return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.BOOLEAN, plain.toString());
        }
        DynamoDbAttributeValueDto result = new DynamoDbAttributeValueDto();
        result.attributeName = name;
        if (plain == null) {
            result.type = DynamoDbValueTypeDto.NULL;
        } else if (plain instanceof Map) {
            result.type = DynamoDbValueTypeDto.MAP;
            result.mapValue = new LinkedHashMap<>();
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) plain).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("DynamoDB map keys must be strings");
                }
                sorted.put((String) entry.getKey(), entry.getValue());
            }
            sorted.forEach((key, child) -> result.mapValue.put(key, fromPlainValue(key, child)));
        } else if (plain instanceof List) {
            result.type = DynamoDbValueTypeDto.LIST;
            result.listValue = new ArrayList<>();
            for (Object child : (List<?>) plain) {
                result.listValue.add(fromPlainValue(null, child));
            }
        } else if (plain instanceof Set) {
            populateSetValue(result, (Set<?>) plain);
        } else {
            throw new IllegalArgumentException("Unsupported DynamoDB template value: " + plain.getClass().getName());
        }
        return result;
    }

    private static String formatFiniteNumber(Number number) {
        if (number instanceof Double && !Double.isFinite((Double) number)) {
            throw new IllegalArgumentException("Unsupported non-finite DynamoDB number value: " + number);
        }
        if (number instanceof Float && !Float.isFinite((Float) number)) {
            throw new IllegalArgumentException("Unsupported non-finite DynamoDB number value: " + number);
        }
        if (number instanceof BigDecimal) {
            return ((BigDecimal) number).toPlainString();
        }
        return number.toString();
    }

    private static void populateSetValue(DynamoDbAttributeValueDto result, Set<?> inputSet) {
        if (inputSet.isEmpty()) {
            throw new IllegalArgumentException("DynamoDB sets cannot be empty");
        }
        Object first = inputSet.iterator().next();
        if (first instanceof String) {
            TreeSet<String> sorted = new TreeSet<>();
            for (Object element : inputSet) {
                if (!(element instanceof String)) {
                    throw new IllegalArgumentException("DynamoDB string sets must contain only strings");
                }
                sorted.add((String) element);
            }
            result.type = DynamoDbValueTypeDto.STRING_SET;
            result.setValue = new ArrayList<>(sorted);
        } else if (first instanceof Number) {
            TreeMap<BigDecimal, String> sorted = new TreeMap<>();
            for (Object element : inputSet) {
                if (!(element instanceof Number)) {
                    throw new IllegalArgumentException("DynamoDB number sets must contain only numbers");
                }
                String text = formatFiniteNumber((Number) element);
                BigDecimal numeric;
                try {
                    numeric = new BigDecimal(text);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported DynamoDB number set value: " + text, e);
                }
                sorted.putIfAbsent(numeric, text);
            }
            result.type = DynamoDbValueTypeDto.NUMBER_SET;
            result.setValue = new ArrayList<>(sorted.values());
        } else if (first instanceof byte[]) {
            throw new IllegalArgumentException("Unsupported DynamoDB binary set (BS) value");
        } else {
            throw new IllegalArgumentException("Unsupported DynamoDB set element type: "
                    + (first == null ? "null" : first.getClass().getName()));
        }
    }

    /**
     * Converts a typed tree to Java values at the JSON DSL boundary.
     *
     * @param dto typed attribute value to convert
     * @return scalar, map, list, set, or null, preserving decimal precision
     * @throws IllegalArgumentException if the dto or its attribute type is null or unsupported,
     *                                  or if a set value is empty
     */
    public static Object toPlainValue(DynamoDbAttributeValueDto dto) {
        if (dto == null || dto.type == null) {
            throw new IllegalArgumentException("Unsupported DynamoDB attribute type: null");
        }
        switch (dto.type) {
            case STRING:
                return dto.value;
            case NUMBER:
                return new BigDecimal(dto.value);
            case BOOLEAN:
                return Boolean.valueOf(dto.value);
            case NULL:
                return null;
            case MAP:
                Map<String, Object> map = new LinkedHashMap<>();
                if (dto.mapValue != null) {
                    new TreeMap<>(dto.mapValue).forEach((key, child) -> map.put(key, toPlainValue(child)));
                }
                return map;
            case LIST:
                List<Object> list = new ArrayList<>();
                if (dto.listValue != null) {
                    for (DynamoDbAttributeValueDto child : dto.listValue) {
                        list.add(toPlainValue(child));
                    }
                }
                return list;
            case STRING_SET:
                if (dto.setValue == null || dto.setValue.isEmpty()) {
                    throw new IllegalArgumentException("DynamoDB sets cannot be empty");
                }
                return new LinkedHashSet<>(new TreeSet<>(dto.setValue));
            case NUMBER_SET:
                if (dto.setValue == null || dto.setValue.isEmpty()) {
                    throw new IllegalArgumentException("DynamoDB sets cannot be empty");
                }
                TreeSet<BigDecimal> numbers = new TreeSet<>();
                for (String element : dto.setValue) {
                    numbers.add(new BigDecimal(element));
                }
                return new LinkedHashSet<>(numbers);
            default:
                throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + dto.type);
        }
    }
}
