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
 * Serializable, typed value copied from one DynamoDB request attribute.
 */
public final class DynamoDbQueryValue implements Serializable {

    private final DynamoDbAttributeType type;
    private final Serializable value;

    private DynamoDbQueryValue(DynamoDbAttributeType type, Serializable value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a value with no captured SDK type, distinct from an explicit DynamoDB null.
     *
     * @return unset query value
     */
    public static DynamoDbQueryValue unset() {
        return new DynamoDbQueryValue(DynamoDbAttributeType.UNSET, null);
    }

    /**
     * Creates a DynamoDB null value.
     *
     * @return null query value
     */
    public static DynamoDbQueryValue nullValue() {
        return new DynamoDbQueryValue(DynamoDbAttributeType.NULL, null);
    }

    /**
     * Creates a string value.
     *
     * @param value string value
     * @return string query value
     */
    public static DynamoDbQueryValue string(String value) {
        return new DynamoDbQueryValue(DynamoDbAttributeType.STRING, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a number value while preserving its exact SDK string form.
     *
     * @param value number string
     * @return number query value
     */
    public static DynamoDbQueryValue number(String value) {
        return new DynamoDbQueryValue(DynamoDbAttributeType.NUMBER, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a boolean value.
     *
     * @param value boolean value
     * @return boolean query value
     */
    public static DynamoDbQueryValue bool(Boolean value) {
        return new DynamoDbQueryValue(DynamoDbAttributeType.BOOLEAN, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a binary value.
     *
     * @param value binary value
     * @return binary query value
     */
    public static DynamoDbQueryValue binary(byte[] value) {
        return new DynamoDbQueryValue(DynamoDbAttributeType.BINARY, Objects.requireNonNull(value, "value").clone());
    }

    /**
     * Creates a string set.
     *
     * @param values set values
     * @return string-set query value
     */
    public static DynamoDbQueryValue stringSet(Collection<String> values) {
        return new DynamoDbQueryValue(DynamoDbAttributeType.STRING_SET, (Serializable) immutableSet(values));
    }

    /**
     * Creates a number set while preserving each exact SDK string form.
     *
     * @param values set values
     * @return number-set query value
     */
    public static DynamoDbQueryValue numberSet(Collection<String> values) {
        return new DynamoDbQueryValue(DynamoDbAttributeType.NUMBER_SET, (Serializable) immutableSet(values));
    }

    /**
     * Creates a binary set.
     *
     * @param values binary set values
     * @return binary-set query value
     */
    public static DynamoDbQueryValue binarySet(Collection<byte[]> values) {
        Objects.requireNonNull(values, "values");
        List<byte[]> copy = new ArrayList<>();
        for (byte[] element : values) {
            copy.add(Objects.requireNonNull(element, "binary set element").clone());
        }
        return new DynamoDbQueryValue(DynamoDbAttributeType.BINARY_SET, (Serializable) Collections.unmodifiableList(copy));
    }

    /**
     * Creates a map value.
     *
     * @param values attribute name to typed query value map
     * @return map query value
     */
    public static DynamoDbQueryValue map(Map<String, DynamoDbQueryValue> values) {
        Objects.requireNonNull(values, "values");
        Map<String, DynamoDbQueryValue> copy = new LinkedHashMap<>();
        for (Map.Entry<String, DynamoDbQueryValue> entry : values.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "map key"),
                    Objects.requireNonNull(entry.getValue(), "map value"));
        }
        return new DynamoDbQueryValue(DynamoDbAttributeType.MAP, (Serializable) Collections.unmodifiableMap(copy));
    }

    /**
     * Creates a list value.
     *
     * @param values list values
     * @return list query value
     */
    public static DynamoDbQueryValue list(Collection<DynamoDbQueryValue> values) {
        Objects.requireNonNull(values, "values");
        List<DynamoDbQueryValue> copy = new ArrayList<>();
        for (DynamoDbQueryValue value : values) {
            copy.add(Objects.requireNonNull(value, "list value"));
        }
        return new DynamoDbQueryValue(DynamoDbAttributeType.LIST, (Serializable) Collections.unmodifiableList(copy));
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
