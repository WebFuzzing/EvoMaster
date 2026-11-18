package org.evomaster.client.java.controller.dynamodb.dsl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * DSL for DynamoDB insertions in generated tests.
 * Binary ({@code B}) and binary sets ({@code BS}) are intentionally unsupported.
 */
public final class DynamoDbDsl implements DynamoDbSequenceDsl, DynamoDbStatementDsl {

    private static final ObjectMapper DDB_JSON_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private List<DynamoDbInsertionDto> insertions = new ArrayList<>();
    private final List<DynamoDbInsertionDto> previousInsertionDtos = new ArrayList<>();
    private DynamoDbInsertionDto current;

    private DynamoDbDsl() {
    }

    private DynamoDbDsl(List<DynamoDbInsertionDto>... previous) {
        if (previous != null && previous.length > 0) {
            Arrays.stream(previous).forEach(previousInsertionDtos::addAll);
        }
    }

    /**
     * @return a new DynamoDB insertion sequence
     */
    public static DynamoDbSequenceDsl dynamoDb() {
        return new DynamoDbDsl();
    }

    /**
     * @param previous insertion sequences executed before this one
     * @return a new DynamoDB insertion sequence
     */
    @SafeVarargs
    public static DynamoDbSequenceDsl dynamoDb(List<DynamoDbInsertionDto>... previous) {
        return new DynamoDbDsl(previous);
    }

    @Override
    public DynamoDbStatementDsl insertInto(String tableName) {
        checkOpen();
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified table");
        }
        current = new DynamoDbInsertionDto();
        current.tableName = tableName;
        insertions.add(current);
        return this;
    }

    @Override
    public DynamoDbStatementDsl d(String attributeName, String printableValue) {
        if (printableValue == null) {
            throw new IllegalArgumentException("Unspecified attribute value");
        }
        if (isStringLiteral(printableValue)) {
            String value = printableValue.substring(1, printableValue.length() - 1).replace("''", "'");
            return attribute(attributeName, DynamoDbKeyScalarTypeDto.STRING, value);
        }
        if ("true".equalsIgnoreCase(printableValue) || "false".equalsIgnoreCase(printableValue)) {
            return attribute(attributeName, DynamoDbKeyScalarTypeDto.BOOLEAN,
                    Boolean.toString(Boolean.parseBoolean(printableValue)));
        }
        try {
            new BigDecimal(printableValue);
            return attribute(attributeName, DynamoDbKeyScalarTypeDto.NUMBER, printableValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unsupported DynamoDB scalar value: " + printableValue, e);
        }
    }

    /**
     * Adds an attribute parsed from AWS DynamoDB JSON representation, supporting nested documents
     * ({@code M}, {@code L}), sets ({@code SS}, {@code NS}), scalars ({@code S}, {@code N}, {@code BOOL}),
     * and {@code NULL}. Binary ({@code B}) and binary sets ({@code BS}) are intentionally unsupported.
     *
     * @param attributeName attribute name
     * @param ddbJson serialized DynamoDB JSON value (e.g. {@code {"L":[{"SS":["a","b"]}]}})
     * @return this statement
     * @throws IllegalArgumentException if {@code attributeName} or {@code ddbJson} is invalid or unsupported
     * @throws IllegalStateException if called before {@link #insertInto(String)} or after {@link #dtos()}
     */
    @Override
    public DynamoDbStatementDsl ddbJson(String attributeName, String ddbJson) {
        checkOpen();
        if (current == null) {
            throw new IllegalStateException("Call insertInto before adding attributes");
        }
        if (attributeName == null || attributeName.isEmpty() || ddbJson == null) {
            throw new IllegalArgumentException("Unspecified DynamoDB JSON attribute");
        }
        try {
            Object parsed = DDB_JSON_MAPPER.readValue(ddbJson, Object.class);
            current.attributes.add(fromDynamoDbJson(attributeName, parsed));
            return this;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid DynamoDB JSON attribute", e);
        }
    }

    @Override
    public List<DynamoDbInsertionDto> dtos() {
        checkOpen();
        List<DynamoDbInsertionDto> result = insertions;
        insertions = null;
        current = null;
        return result;
    }

    private static DynamoDbAttributeValueDto fromDynamoDbJson(String name, Object node) {
        if (!(node instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("DynamoDB JSON value must be an object");
        }
        Map<?, ?> wrapper = (Map<?, ?>) node;
        if (wrapper.size() != 1) {
            throw new IllegalArgumentException("DynamoDB JSON value must have exactly one type key");
        }
        Map.Entry<?, ?> entry = wrapper.entrySet().iterator().next();
        if (!(entry.getKey() instanceof String)) {
            throw new IllegalArgumentException("DynamoDB JSON type key must be a string");
        }
        DynamoDbValueTypeDto type = DynamoDbValueTypeDto.fromWireCode((String) entry.getKey());
        Object rawValue = entry.getValue();

        switch (type) {
            case STRING:
                if (!(rawValue instanceof String)) {
                    throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be a string");
                }
                return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.STRING, (String) rawValue);
            case NUMBER:
                if (!(rawValue instanceof String)) {
                    throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be a numeric string");
                }
                String numericText = (String) rawValue;
                parseBigDecimal(numericText);
                return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.NUMBER, numericText);
            case BOOLEAN:
                if (!(rawValue instanceof Boolean)) {
                    throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be a boolean");
                }
                return new DynamoDbAttributeValueDto(name, DynamoDbValueTypeDto.BOOLEAN, rawValue.toString());
            case NULL:
                if (!Boolean.TRUE.equals(rawValue)) {
                    throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be true");
                }
                DynamoDbAttributeValueDto nullDto = new DynamoDbAttributeValueDto();
                nullDto.attributeName = name;
                nullDto.type = DynamoDbValueTypeDto.NULL;
                return nullDto;
            case MAP:
                if (!(rawValue instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be an object");
                }
                DynamoDbAttributeValueDto mapDto = new DynamoDbAttributeValueDto();
                mapDto.attributeName = name;
                mapDto.type = DynamoDbValueTypeDto.MAP;
                mapDto.mapValue = new LinkedHashMap<>();
                Map<String, Object> sortedMap = new TreeMap<>();
                for (Map.Entry<?, ?> child : ((Map<?, ?>) rawValue).entrySet()) {
                    if (!(child.getKey() instanceof String)) {
                        throw new IllegalArgumentException("DynamoDB map keys must be strings");
                    }
                    sortedMap.put((String) child.getKey(), child.getValue());
                }
                sortedMap.forEach((key, child) -> mapDto.mapValue.put(key, fromDynamoDbJson(key, child)));
                return mapDto;
            case LIST:
                if (!(rawValue instanceof List<?>)) {
                    throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be an array");
                }
                DynamoDbAttributeValueDto listDto = new DynamoDbAttributeValueDto();
                listDto.attributeName = name;
                listDto.type = DynamoDbValueTypeDto.LIST;
                listDto.listValue = new ArrayList<>();
                for (Object child : (List<?>) rawValue) {
                    listDto.listValue.add(fromDynamoDbJson(null, child));
                }
                return listDto;
            case STRING_SET:
                return parseSet(name, type, rawValue, Function.identity());
            case NUMBER_SET:
                return parseSet(name, type, rawValue, DynamoDbDsl::parseBigDecimal);
            default:
                throw new IllegalArgumentException("Unsupported DynamoDB JSON type: " + type);
        }
    }

    private static <K extends Comparable<? super K>> DynamoDbAttributeValueDto parseSet(
            String name,
            DynamoDbValueTypeDto type,
            Object rawValue,
            Function<String, K> keyExtractor) {
        if (!(rawValue instanceof List<?>)) {
            throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " value must be an array");
        }
        List<?> elements = (List<?>) rawValue;
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("DynamoDB sets cannot be empty");
        }
        TreeMap<K, String> sorted = new TreeMap<>();
        for (Object element : elements) {
            if (!(element instanceof String)) {
                throw new IllegalArgumentException("DynamoDB JSON " + type.wireCode + " elements must be strings");
            }
            String text = (String) element;
            sorted.putIfAbsent(keyExtractor.apply(text), text);
        }
        DynamoDbAttributeValueDto dto = new DynamoDbAttributeValueDto();
        dto.attributeName = name;
        dto.type = type;
        dto.setValue = new ArrayList<>(sorted.values());
        return dto;
    }

    private static BigDecimal parseBigDecimal(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid DynamoDB number value: " + text, e);
        }
    }

    /**
     * Appends a scalar attribute value to the current insertion.
     *
     * @param name attribute name
     * @param type scalar attribute type
     * @param value scalar value text
     * @return this statement
     * @throws IllegalArgumentException if {@code name} is null or empty
     * @throws IllegalStateException if called before {@link #insertInto(String)}
     */
    private DynamoDbStatementDsl attribute(String name, DynamoDbKeyScalarTypeDto type, String value) {
        checkOpen();
        if (current == null) {
            throw new IllegalStateException("Call insertInto before adding attributes");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Unspecified attribute name");
        }
        current.attributes.add(new DynamoDbAttributeValueDto(name, type, value));
        return this;
    }

    /**
     * @param printableValue candidate printable string
     * @return {@code true} if enclosed in single quotes
     */
    private boolean isStringLiteral(String printableValue) {
        return printableValue.length() >= 2
                && printableValue.startsWith("'")
                && printableValue.endsWith("'");
    }

    /**
     * Verifies that this builder has not already produced its DTO list.
     *
     * @throws IllegalStateException if DTOs have already been consumed
     */
    private void checkOpen() {
        if (insertions == null) {
            throw new IllegalStateException("This DynamoDB DSL sequence has already produced DTOs");
        }
    }
}
