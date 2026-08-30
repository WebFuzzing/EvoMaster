package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;

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
    public DynamoDbStatementDsl s(String name, String value) {
        return attribute(name, DynamoDbScalarTypeDto.S, value);
    }

    @Override
    public DynamoDbStatementDsl n(String name, String value) {
        return attribute(name, DynamoDbScalarTypeDto.N, value);
    }

    @Override
    public DynamoDbStatementDsl bool(String name, boolean value) {
        return attribute(name, DynamoDbScalarTypeDto.BOOL, Boolean.toString(value));
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

    private void checkOpen() {
        if (insertions == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }
}
