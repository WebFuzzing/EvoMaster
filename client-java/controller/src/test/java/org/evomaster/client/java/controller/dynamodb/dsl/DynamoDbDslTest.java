package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertInsertion(insertions.get(0), "WorldCupPlayers", "country", DynamoDbKeyScalarTypeDto.STRING, "Argentina");
        assertInsertion(insertions.get(0), "WorldCupPlayers", "fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "10");
        assertInsertion(insertions.get(0), "WorldCupPlayers", "captain", DynamoDbKeyScalarTypeDto.BOOLEAN, "true");
        assertInsertion(insertions.get(1), "WorldCupPlayers", "country", DynamoDbKeyScalarTypeDto.STRING, "Brazil");
        assertInsertion(insertions.get(1), "WorldCupPlayers", "fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "1");
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

        assertInsertion(insertion, "WorldCupPlayers", "shirtNumber", DynamoDbKeyScalarTypeDto.STRING, "10");
        assertInsertion(insertion, "WorldCupPlayers", "captainLabel", DynamoDbKeyScalarTypeDto.STRING, "true");
        assertInsertion(insertion, "WorldCupPlayers", "goals", DynamoDbKeyScalarTypeDto.NUMBER, "3e1");
        assertInsertion(insertion, "WorldCupPlayers", "captain", DynamoDbKeyScalarTypeDto.BOOLEAN, "true");
    }

    @Test
    public void testAcceptsPreviousWorldCupPlayerInsertions() {
        List<DynamoDbInsertionDto> previous = DynamoDbDsl.dynamoDb()
                .insertInto("WorldCupPlayers")
                .d("country", "'Argentina'")
                .dtos();

        List<DynamoDbInsertionDto> current = DynamoDbDsl.dynamoDb(previous)
                .insertInto("WorldCupPlayers")
                .d("country", "'Brazil'")
                .dtos();

        assertEquals(1, current.size());
        assertInsertion(current.get(0), "WorldCupPlayers", "country", DynamoDbKeyScalarTypeDto.STRING, "Brazil");
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
            DynamoDbKeyScalarTypeDto type,
            String value) {
        assertEquals(tableName, insertion.tableName);
        DynamoDbAttributeValueDto attribute = insertion.attributes.stream()
                .filter(candidate -> attributeName.equals(candidate.attributeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing attribute " + attributeName));
        assertEquals(DynamoDbValueTypeDto.fromKeyScalar(type), attribute.type);
        assertEquals(value, attribute.value);
    }
    /** JSON is parsed once at the DSL boundary into a recursive typed DTO. */
    @Test
    public void testJsonBoundaryCreatesTypedPayload() {
        DynamoDbAttributeValueDto value = DynamoDbDsl.dynamoDb().insertInto("players")
                .json("profile", "{\"players\":[\"Lionel Messi\",null],\"score\":10.1234567890123456789}")
                .dtos().get(0).attributes.get(0);
        assertEquals(DynamoDbValueTypeDto.MAP, value.type);
        assertNull(value.value);
        assertEquals("10.1234567890123456789", value.mapValue.get("score").value);
        assertEquals(DynamoDbValueTypeDto.NULL,
                value.mapValue.get("players").listValue.get(1).type);
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb().insertInto("players").json("profile", "{} garbage"));
    }
    /** The controller JSON transport retains the typed tree instead of embedding another JSON string. */
    @Test
    public void testTypedTransportRoundTrip() throws Exception {
        DynamoDbAttributeValueDto original = DynamoDbDsl.dynamoDb().insertInto("players")
                .json("profile", "{\"name\":\"Lionel Messi\",\"teams\":[],\"coach\":null}")
                .dtos().get(0).attributes.get(0);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        DynamoDbAttributeValueDto transported = mapper.readValue(mapper.writeValueAsString(original), DynamoDbAttributeValueDto.class);
        assertEquals(original, transported);
        assertNull(transported.value);
        assertEquals("Lionel Messi", transported.mapValue.get("name").value);
    }
}
