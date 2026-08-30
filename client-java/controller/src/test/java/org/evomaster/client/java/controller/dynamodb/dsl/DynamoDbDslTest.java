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
                .s("country", "Argentina")
                .n("fifaId", "10")
                .bool("captain", true)
                .insertInto("WorldCupPlayers")
                .s("country", "Brazil")
                .n("fifaId", "1")
                .dtos();

        assertEquals(2, insertions.size());
        assertInsertion(insertions.get(0), "WorldCupPlayers", "country", DynamoDbScalarTypeDto.S, "Argentina");
        assertInsertion(insertions.get(0), "WorldCupPlayers", "fifaId", DynamoDbScalarTypeDto.N, "10");
        assertInsertion(insertions.get(0), "WorldCupPlayers", "captain", DynamoDbScalarTypeDto.BOOL, "true");
        assertInsertion(insertions.get(1), "WorldCupPlayers", "country", DynamoDbScalarTypeDto.S, "Brazil");
        assertInsertion(insertions.get(1), "WorldCupPlayers", "fifaId", DynamoDbScalarTypeDto.N, "1");
    }

    @Test
    public void testRejectsIncompleteInsertionDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb().insertInto(null));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb().insertInto(""));

        DynamoDbStatementDsl statement = (DynamoDbStatementDsl) DynamoDbDsl.dynamoDb();
        assertThrows(IllegalStateException.class, () -> statement.s("country", "Argentina"));

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
