package org.evomaster.client.java.controller.dynamodb.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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

    /** DynamoDB JSON is parsed into recursive typed DTOs, including sets nested inside lists and maps. */
    @Test
    public void testDdbJsonSupportsNestedDocumentsAndSets() {
        DynamoDbInsertionDto insertion = DynamoDbDsl.dynamoDb().insertInto("WorldCupPlayers")
                .ddbJson("profile",
                        "{\"M\":{\"players\":{\"L\":[{\"S\":\"Lionel Messi\"},{\"NULL\":true}]},"
                                + "\"score\":{\"N\":\"10.1234567890123456789\"},"
                                + "\"captain\":{\"BOOL\":true}}}")
                .ddbJson("rolesAndNumbers",
                        "{\"L\":[{\"SS\":[\"forward\",\"captain\",\"forward\"]},"
                                + "{\"NS\":[\"30.5\",\"10\",\"10.0\"]}]}")
                .ddbJson("positions", "{\"SS\":[\"forward\",\"captain\"]}")
                .ddbJson("jerseyNumbers", "{\"NS\":[\"10\",\"30.5\"]}")
                .dtos()
                .get(0);

        DynamoDbAttributeValueDto profile = insertion.attributes.get(0);
        assertEquals(DynamoDbValueTypeDto.MAP, profile.type);
        assertNull(profile.value);
        assertEquals("10.1234567890123456789", profile.mapValue.get("score").value);
        assertEquals("true", profile.mapValue.get("captain").value);
        assertEquals("Lionel Messi", profile.mapValue.get("players").listValue.get(0).value);
        assertEquals(DynamoDbValueTypeDto.NULL, profile.mapValue.get("players").listValue.get(1).type);

        DynamoDbAttributeValueDto nested = insertion.attributes.get(1);
        assertEquals(DynamoDbValueTypeDto.LIST, nested.type);
        assertEquals(DynamoDbValueTypeDto.STRING_SET, nested.listValue.get(0).type);
        assertEquals(Arrays.asList("captain", "forward"), nested.listValue.get(0).setValue);
        assertEquals(DynamoDbValueTypeDto.NUMBER_SET, nested.listValue.get(1).type);
        assertEquals(Arrays.asList("10", "30.5"), nested.listValue.get(1).setValue);

        assertEquals(DynamoDbValueTypeDto.STRING_SET, insertion.attributes.get(2).type);
        assertEquals(Arrays.asList("captain", "forward"), insertion.attributes.get(2).setValue);
        assertEquals(DynamoDbValueTypeDto.NUMBER_SET, insertion.attributes.get(3).type);
        assertEquals(Arrays.asList("10", "30.5"), insertion.attributes.get(3).setValue);
    }

    @Test
    public void testDdbJsonRejectsInvalidAndBinaryPayloads() {
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("profile", "{} garbage"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("profile", "{}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("profile", "{\"S\":\"a\",\"N\":\"1\"}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("flag", "{\"B\":\"AQID\"}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("flags", "{\"BS\":[\"AQID\"]}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("emptySet", "{\"SS\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("badNum", "{\"N\":10}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("badNumSet", "{\"NS\":[\"ten\"]}"));
        assertThrows(IllegalArgumentException.class, () -> DynamoDbDsl.dynamoDb()
                .insertInto("players").ddbJson("badNull", "{\"NULL\":false}"));
    }

    /** The controller JSON transport retains the typed tree instead of embedding another JSON string. */
    @Test
    public void testTypedTransportRoundTrip() throws Exception {
        DynamoDbInsertionDto original = DynamoDbDsl.dynamoDb().insertInto("players")
                .ddbJson("profile", "{\"M\":{\"name\":{\"S\":\"Lionel Messi\"},\"teams\":{\"L\":[]},\"coach\":{\"NULL\":true}}}")
                .ddbJson("rolesAndNumbers", "{\"L\":[{\"SS\":[\"forward\",\"captain\"]},{\"NS\":[\"10\",\"30.5\"]}]}")
                .dtos().get(0);
        ObjectMapper mapper = new ObjectMapper();
        DynamoDbInsertionDto transported = mapper.readValue(mapper.writeValueAsString(original), DynamoDbInsertionDto.class);
        assertEquals(original.attributes, transported.attributes);
        assertNull(transported.attributes.get(0).value);
        assertEquals("Lionel Messi", transported.attributes.get(0).mapValue.get("name").value);
        assertEquals(Arrays.asList("captain", "forward"),
                transported.attributes.get(1).listValue.get(0).setValue);
        assertEquals(Arrays.asList("10", "30.5"),
                transported.attributes.get(1).listValue.get(1).setValue);
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
}
