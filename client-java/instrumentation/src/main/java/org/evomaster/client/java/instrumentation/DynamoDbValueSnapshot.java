package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Serializable, canonical representation of one DynamoDB attribute value.
 */
public final class DynamoDbValueSnapshot implements Serializable {

    private final DynamoDbAttributeType type;
    private final Serializable value;

    private DynamoDbValueSnapshot(DynamoDbAttributeType type, Serializable value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates an unset value.
     *
     * @return unset value snapshot
     */
    public static DynamoDbValueSnapshot unset() {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.UNSET, null);
    }

    /**
     * Creates a DynamoDB null value.
     *
     * @return null value snapshot
     */
    public static DynamoDbValueSnapshot nullValue() {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.NULL, null);
    }

    /**
     * Creates a string value.
     *
     * @param value string value
     * @return string snapshot
     */
    public static DynamoDbValueSnapshot string(String value) {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.STRING, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a number value while preserving its exact SDK string form.
     *
     * @param value number string
     * @return number snapshot
     */
    public static DynamoDbValueSnapshot number(String value) {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.NUMBER, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a boolean value.
     *
     * @param value boolean value
     * @return boolean snapshot
     */
    public static DynamoDbValueSnapshot bool(Boolean value) {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.BOOLEAN, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a binary value.
     *
     * @param value binary value
     * @return binary snapshot
     */
    public static DynamoDbValueSnapshot binary(byte[] value) {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.BINARY, Objects.requireNonNull(value, "value").clone());
    }

    /**
     * Creates a string set.
     *
     * @param values set values
     * @return string-set snapshot
     */
    public static DynamoDbValueSnapshot stringSet(Collection<String> values) {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.STRING_SET, (Serializable) immutableSet(values));
    }

    /**
     * Creates a number set while preserving each exact SDK string form.
     *
     * @param values set values
     * @return number-set snapshot
     */
    public static DynamoDbValueSnapshot numberSet(Collection<String> values) {
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.NUMBER_SET, (Serializable) immutableSet(values));
    }

    /**
     * Creates a binary set.
     *
     * @param values binary set values
     * @return binary-set snapshot
     */
    public static DynamoDbValueSnapshot binarySet(Collection<byte[]> values) {
        Objects.requireNonNull(values, "values");
        List<byte[]> copy = new ArrayList<>();
        for (byte[] element : values) {
            copy.add(Objects.requireNonNull(element, "binary set element").clone());
        }
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.BINARY_SET, (Serializable) Collections.unmodifiableList(copy));
    }

    /**
     * Creates a map value.
     *
     * @param values map values
     * @return map snapshot
     */
    public static DynamoDbValueSnapshot map(Map<String, DynamoDbValueSnapshot> values) {
        Objects.requireNonNull(values, "values");
        Map<String, DynamoDbValueSnapshot> copy = new LinkedHashMap<>();
        for (Map.Entry<String, DynamoDbValueSnapshot> entry : values.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "map key"),
                    Objects.requireNonNull(entry.getValue(), "map value"));
        }
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.MAP, (Serializable) Collections.unmodifiableMap(copy));
    }

    /**
     * Creates a list value.
     *
     * @param values list values
     * @return list snapshot
     */
    public static DynamoDbValueSnapshot list(Collection<DynamoDbValueSnapshot> values) {
        Objects.requireNonNull(values, "values");
        List<DynamoDbValueSnapshot> copy = new ArrayList<>();
        for (DynamoDbValueSnapshot value : values) {
            copy.add(Objects.requireNonNull(value, "list value"));
        }
        return new DynamoDbValueSnapshot(DynamoDbAttributeType.LIST, (Serializable) Collections.unmodifiableList(copy));
    }

    /**
     * Returns the DynamoDB type.
     *
     * @return value type
     */
    public DynamoDbAttributeType type() {
        return type;
    }

    /**
     * Returns the validated serializable payload for the type.
     *
     * @return payload, or {@code null} for null and unset values
     */
    public Serializable value() {
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        if (value instanceof List<?> && type == DynamoDbAttributeType.BINARY_SET) {
            List<byte[]> copy = new ArrayList<>();
            for (Object element : (List<?>) value) {
                byte[] bytes = (byte[]) element;
                copy.add(bytes == null ? null : bytes.clone());
            }
            return (Serializable) Collections.unmodifiableList(copy);
        }
        return value;
    }

    private static Set<String> immutableSet(Collection<String> values) {
        Objects.requireNonNull(values, "values");
        Set<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            copy.add(Objects.requireNonNull(value, "set value"));
        }
        return Collections.unmodifiableSet(copy);
    }
}
