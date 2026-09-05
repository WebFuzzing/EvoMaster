package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the DynamoDB insertion DTOs produced by the generated-test DSL. */
public class DynamoDbDslTest {

    @Test
    public void testBuildsWorldCupPlayerInsertions() {
        List<DynamoDbInsertionDto> insertions = DynamoDbDsl.dynamoDb()
                .insertInto("WorldCupPlayers")
                .d("country", "'Argentina'")
                .d("fifaId", "10")
                .d("captain", "true")
                .insertInto("WorldCupPlayers")
                .d("country", "'Brazil'")
                .d("fifaId", "1")
                .dtos();

        assertEquals(2, insertions.size());
        assertInsertion(insertions.get(0), "WorldCupPlayers", "country", DynamoDbScalarTypeDto.STRING, "Argentina");
        assertInsertion(insertions.get(0), "WorldCupPlayers", "fifaId", DynamoDbScalarTypeDto.NUMBER, "10");
        assertInsertion(insertions.get(0), "WorldCupPlayers", "captain", DynamoDbScalarTypeDto.BOOLEAN, "true");
        assertInsertion(insertions.get(1), "WorldCupPlayers", "country", DynamoDbScalarTypeDto.STRING, "Brazil");
        assertInsertion(insertions.get(1), "WorldCupPlayers", "fifaId", DynamoDbScalarTypeDto.NUMBER, "1");
    }

    @Test
    public void testDistinguishesQuotedWorldCupPlayerValues() {
        DynamoDbInsertionDto insertion = DynamoDbDsl.dynamoDb()
                .insertInto("WorldCupPlayers")
                .d("shirtNumber", "'10'")
                .d("captainLabel", "'true'")
                .d("goals", "3e1")
                .d("captain", "TRUE")
                .dtos()
                .get(0);

        assertInsertion(insertion, "WorldCupPlayers", "shirtNumber", DynamoDbScalarTypeDto.STRING, "10");
        assertInsertion(insertion, "WorldCupPlayers", "captainLabel", DynamoDbScalarTypeDto.STRING, "true");
        assertInsertion(insertion, "WorldCupPlayers", "goals", DynamoDbScalarTypeDto.NUMBER, "3e1");
        assertInsertion(insertion, "WorldCupPlayers", "captain", DynamoDbScalarTypeDto.BOOLEAN, "true");
    }

    @Test
    public void testRejectsIncompleteInsertionDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb().insertInto(null));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb().insertInto(""));

        DynamoDbStatementDsl statement = (DynamoDbStatementDsl) DynamoDbDsl.dynamoDb();
        assertThrows(IllegalStateException.class, () -> statement.d("country", "'Argentina'"));

        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("WorldCupPlayers").d("country", null));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("WorldCupPlayers").d("country", "Argentina"));

        DynamoDbStatementDsl completed = DynamoDbDsl.dynamoDb().insertInto("WorldCupPlayers");
        completed.dtos();
        assertThrows(IllegalStateException.class, () -> completed.insertInto("WorldCupPlayers"));
    }

    private void assertInsertion(
            DynamoDbInsertionDto insertion,
            String tableName,
            String attributeName,
            DynamoDbScalarTypeDto type,
            String value) {
        assertEquals(tableName, insertion.tableName);
        DynamoDbAttributeValueDto attribute = insertion.attributes.stream()
                .filter(candidate -> attributeName.equals(candidate.attributeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing attribute " + attributeName));
        assertEquals(type, attribute.type);
        assertEquals(value, attribute.value);
    }
}
