package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DSL for DynamoDB insertions in generated tests.
 */
public final class DynamoDbDsl implements DynamoDbSequenceDsl, DynamoDbStatementDsl {

    private List<DynamoDbInsertionDto> insertions = new ArrayList<>();
    private DynamoDbInsertionDto current;

    private DynamoDbDsl() {
    }

    /**
     * @return a new DynamoDB insertion sequence
     */
    public static DynamoDbSequenceDsl dynamoDb() {
        return new DynamoDbDsl();
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
            return attribute(attributeName, DynamoDbScalarTypeDto.STRING, value);
        }
        if ("true".equalsIgnoreCase(printableValue) || "false".equalsIgnoreCase(printableValue)) {
            return attribute(attributeName, DynamoDbScalarTypeDto.BOOLEAN,
                    Boolean.toString(Boolean.parseBoolean(printableValue)));
        }
        try {
            new BigDecimal(printableValue);
            return attribute(attributeName, DynamoDbScalarTypeDto.NUMBER, printableValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unsupported DynamoDB scalar value: " + printableValue, e);
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

    private DynamoDbStatementDsl attribute(String name, DynamoDbScalarTypeDto type, String value) {
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
     * @param value value to inspect
     * @return whether the value is enclosed in single quotes
     */
    private boolean isStringLiteral(String value) {
        return value.length() >= 2 && value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'';
    }

    private void checkOpen() {
        if (insertions == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }
}
