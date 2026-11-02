package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DSL for DynamoDB insertions in generated tests.
 */
public final class DynamoDbDsl implements DynamoDbSequenceDsl, DynamoDbStatementDsl {

    /** Accumulated insertions built by this DSL instance. */
    private List<DynamoDbInsertionDto> insertions = new ArrayList<>();

    /** Insertion DTOs from prior sequences that executed before this one. */
    private final List<DynamoDbInsertionDto> previousInsertionDtos = new ArrayList<>();

    /** The insertion currently being constructed. */
    private DynamoDbInsertionDto current;

    /**
     * Creates an empty DSL sequence.
     */
    private DynamoDbDsl() {
    }

    /**
     * Creates a DSL sequence chained after previous insertion results.
     *
     * @param previous insertion sequences executed before this one
     */
    private DynamoDbDsl(List<DynamoDbInsertionDto>... previous) {
        if (previous != null && previous.length > 0) {
            Arrays.stream(previous).forEach(previousInsertionDtos::addAll);
        }
    }

    /**
     * Starts a new DynamoDB insertion sequence.
     *
     * @return a new DynamoDB insertion sequence
     */
    public static DynamoDbSequenceDsl dynamoDb() {
        return new DynamoDbDsl();
    }

    /**
     * Starts a new DynamoDB insertion sequence chained after previous insertions.
     *
     * @param previous insertion sequences executed before this one
     * @return a new DynamoDB insertion sequence
     */
    @SafeVarargs
    public static DynamoDbSequenceDsl dynamoDb(List<DynamoDbInsertionDto>... previous) {
        return new DynamoDbDsl(previous);
    }

    /**
     * Starts an insertion into the specified table.
     *
     * @param tableName name of the DynamoDB table
     * @return the statement builder for adding attributes
     * @throws IllegalArgumentException if {@code tableName} is null or empty
     * @throws IllegalStateException if this sequence has already produced DTOs
     */
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

    /**
     * Adds a scalar attribute using its printable representation. Strings must be enclosed in single quotes,
     * numbers are represented by their exact text, and booleans are represented by {@code true} or {@code false}.
     *
     * @param attributeName attribute name
     * @param printableValue scalar value in printable form
     * @return the continuation of this statement
     * @throws IllegalArgumentException if {@code printableValue} is null or malformed
     * @throws IllegalStateException if called before {@link #insertInto(String)} or after {@link #dtos()}
     */
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
     * Adds a document template without interpreting it as a string attribute.
     *
     * @param attributeName attribute name
     * @param json serialized JSON document
     * @return this statement
     * @throws IllegalArgumentException if {@code attributeName} is invalid or {@code json} is malformed
     * @throws IllegalStateException if called before {@link #insertInto(String)} or after {@link #dtos()}
     */
    @Override
    public DynamoDbStatementDsl json(String attributeName, String json) {
        checkOpen();
        if (current == null) throw new IllegalStateException("Call insertInto before adding attributes");
        if (attributeName == null || attributeName.isEmpty() || json == null) {
            throw new IllegalArgumentException("Unspecified DynamoDB JSON attribute");
        }
        try {
            Object plain = new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(json, Object.class);
            current.attributes.add(DynamoDbAttributeValueDto.fromPlainValue(attributeName, plain));
            return this;
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid DynamoDB JSON attribute", e);
        }
    }

    /**
     * Consumes this DSL sequence and returns all accumulated insertion DTOs.
     *
     * @return list of generated insertion DTOs
     * @throws IllegalStateException if this sequence has already produced DTOs
     */
    @Override
    public List<DynamoDbInsertionDto> dtos() {
        checkOpen();
        List<DynamoDbInsertionDto> result = insertions;
        insertions = null;
        current = null;
        return result;
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
     * Tests whether a printable representation is enclosed in single quotes.
     *
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
