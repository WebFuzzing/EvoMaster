package org.evomaster.client.java.controller.api.dto.database.operations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A named DynamoDB value with recursive, native map and list payloads.
 */
public class DynamoDbAttributeValueDto {

    public String attributeName;
    public DynamoDbValueTypeDto type;
    public String value;

    /** Map of child attributes for MAP-typed values. */
    public Map<String, DynamoDbAttributeValueDto> mapValue;

    /** List of child attributes for LIST-typed values. */
    public List<DynamoDbAttributeValueDto> listValue;

    /** Whether this observed template value should remain fixed during search. */
    public boolean isTemplateValue;

    public DynamoDbAttributeValueDto() {
    }

    /**
     * Creates a scalar attribute from schema metadata.
     *
     * @param attributeName attribute name
     * @param type scalar schema type
     * @param value exact scalar text
     */
    public DynamoDbAttributeValueDto(String attributeName, DynamoDbKeyScalarTypeDto type, String value) {
        this(attributeName, type == null ? null : DynamoDbValueTypeDto.fromKeyScalar(type), value);
    }

    /**
     * Creates a scalar attribute value.
     *
     * @param attributeName attribute name
     * @param type native scalar value type
     * @param value exact scalar text
     * @throws IllegalArgumentException if {@code type} is composite or null
     */
    public DynamoDbAttributeValueDto(String attributeName, DynamoDbValueTypeDto type, String value) {
        if (type != null && type != DynamoDbValueTypeDto.STRING && type != DynamoDbValueTypeDto.NUMBER
                && type != DynamoDbValueTypeDto.BOOLEAN) {
            throw new IllegalArgumentException("Use fromPlainValue for composite and null values");
        }
        this.attributeName = attributeName;
        this.type = type;
        this.value = value;
    }

    /**
     * Copies normalized Java values into a typed tree; sets and binary values are unsupported.
     * Map keys are sorted for deterministic template selection.
     *
     * @param name attribute name, or null for a list element
     * @param plain normalized scalar, map, list or null
     * @return independent typed value
     * @throws IllegalArgumentException if a map key is not a string or the type is unsupported
     */
    public static DynamoDbAttributeValueDto fromPlainValue(String name, Object plain) {
        if (plain instanceof String) return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.STRING, (String) plain);
        if (plain instanceof Number) return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.NUMBER, plain.toString());
        if (plain instanceof Boolean) return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.BOOLEAN, plain.toString());
        DynamoDbAttributeValueDto result = new DynamoDbAttributeValueDto();
        result.attributeName = name;
        if (plain == null) {
            result.type = DynamoDbValueTypeDto.NULL;
        } else if (plain instanceof Map) {
            result.type = DynamoDbValueTypeDto.MAP;
            result.mapValue = new LinkedHashMap<>();
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) plain).entrySet()) {
                if (!(entry.getKey() instanceof String)) throw new IllegalArgumentException("DynamoDB map keys must be strings");
                sorted.put((String) entry.getKey(), entry.getValue());
            }
            sorted.forEach((key, child) -> result.mapValue.put(key, fromPlainValue(key, child)));
        } else if (plain instanceof List) {
            result.type = DynamoDbValueTypeDto.LIST;
            result.listValue = new ArrayList<>();
            for (Object child : (List<?>) plain) result.listValue.add(fromPlainValue(null, child));
        } else {
            throw new IllegalArgumentException("Unsupported DynamoDB template value: " + plain.getClass().getName());
        }
        return result;
    }

    /**
     * Converts a typed tree to Java values at the JSON DSL boundary.
     *
     * @return scalar, map, list or null, preserving decimal precision
     * @throws IllegalArgumentException if the attribute type is not supported
     */
    public Object toPlainValue() {
        switch (type) {
            case STRING: return value;
            case NUMBER: return new BigDecimal(value);
            case BOOLEAN: return Boolean.valueOf(value);
            case NULL: return null;
            case MAP:
                Map<String, Object> map = new LinkedHashMap<>();
                new TreeMap<>(mapValue).forEach((key, child) -> map.put(key, child.toPlainValue()));
                return map;
            case LIST:
                List<Object> list = new ArrayList<>();
                for (DynamoDbAttributeValueDto child : listValue) list.add(child.toPlainValue());
                return list;
            default: throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + type);
        }
    }

    /**
     * Creates a recursive copy of this attribute value independent of mutable transport payloads.
     *
     * @return a recursive copy independent of mutable transport payloads
     */
    public DynamoDbAttributeValueDto copy() {
        DynamoDbAttributeValueDto copy = new DynamoDbAttributeValueDto();
        copy.attributeName = this.attributeName;
        copy.type = this.type;
        copy.value = this.value;
        copy.isTemplateValue = this.isTemplateValue;
        if (this.mapValue != null) {
            copy.mapValue = new LinkedHashMap<>();
            for (Map.Entry<String, DynamoDbAttributeValueDto> entry : this.mapValue.entrySet()) {
                copy.mapValue.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().copy());
            }
        }
        if (this.listValue != null) {
            copy.listValue = new ArrayList<>(this.listValue.size());
            for (DynamoDbAttributeValueDto child : this.listValue) {
                copy.listValue.add(child == null ? null : child.copy());
            }
        }
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DynamoDbAttributeValueDto)) return false;
        DynamoDbAttributeValueDto that = (DynamoDbAttributeValueDto) other;
        return Objects.equals(attributeName, that.attributeName) && type == that.type
                && Objects.equals(value, that.value) && Objects.equals(mapValue, that.mapValue)
                && Objects.equals(listValue, that.listValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, type, value, mapValue, listValue);
    }
}
