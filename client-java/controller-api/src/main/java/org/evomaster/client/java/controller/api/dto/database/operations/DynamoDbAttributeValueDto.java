package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A named DynamoDB value with recursive map, list, and non-binary set payloads.
 * Binary ({@code B}) and binary sets ({@code BS}) are intentionally unsupported.
 */
public class DynamoDbAttributeValueDto {

    public String attributeName;
    public DynamoDbValueTypeDto type;
    public String value;

    /** Map of child attributes for MAP-typed values. */
    public Map<String, DynamoDbAttributeValueDto> mapValue;

    /** List of child attributes for LIST-typed values. */
    public List<DynamoDbAttributeValueDto> listValue;

    /** Distinct element texts for STRING_SET- and NUMBER_SET-typed values. */
    public List<String> setValue;

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
     * @throws IllegalArgumentException if {@code type} is composite, a set, or {@code NULL}
     */
    public DynamoDbAttributeValueDto(String attributeName, DynamoDbValueTypeDto type, String value) {
        if (type != null && type != DynamoDbValueTypeDto.STRING && type != DynamoDbValueTypeDto.NUMBER
                && type != DynamoDbValueTypeDto.BOOLEAN) {
            throw new IllegalArgumentException("Use DynamoDbDtoUtils.fromPlainValue for composite, set, and null values");
        }
        this.attributeName = attributeName;
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DynamoDbAttributeValueDto)) return false;
        DynamoDbAttributeValueDto that = (DynamoDbAttributeValueDto) other;
        return Objects.equals(attributeName, that.attributeName) && type == that.type
                && Objects.equals(value, that.value) && Objects.equals(mapValue, that.mapValue)
                && Objects.equals(listValue, that.listValue) && Objects.equals(setValue, that.setValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, type, value, mapValue, listValue, setValue);
    }
}
