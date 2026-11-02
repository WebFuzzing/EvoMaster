package org.evomaster.client.java.controller.api.dto.database.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the canonical representation of inferred DynamoDB insertion keys.
 */
public class DynamoDbInsertionKeyBuilderTest {

    @Test
    public void testBuildsWorldCupPlayerInsertionKey() {
        assertEquals("WorldCupPlayers|country:STRING=Argentina|fifaId:NUMBER=10|captain:BOOLEAN=true",
                DynamoDbInsertionKeyBuilder.fromAttributes("WorldCupPlayers", Arrays.asList(
                        new DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"),
                        new DynamoDbAttributeValueDto("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "10"),
                        new DynamoDbAttributeValueDto("captain", DynamoDbKeyScalarTypeDto.BOOLEAN, "true")
                )));
    }

    @Test
    public void testNestedPlayerIdentity() {
        java.util.Map<String, Object> player = new java.util.LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("country", "Argentina");
        String first = DynamoDbInsertionKeyBuilder.fromAttributes("players", java.util.Collections.singletonList(
                DynamoDbAttributeValueDto.fromPlainValue("profile", player)));
        java.util.Map<String, Object> reversed = new java.util.LinkedHashMap<>();
        reversed.put("country", "Argentina");
        reversed.put("name", "Lionel Messi");
        assertEquals(first, DynamoDbInsertionKeyBuilder.fromAttributes("players", java.util.Collections.singletonList(
                DynamoDbAttributeValueDto.fromPlainValue("profile", reversed))));
        reversed.put("name", "Angel Di Maria");
        org.junit.jupiter.api.Assertions.assertNotEquals(first, DynamoDbInsertionKeyBuilder.fromAttributes("players", java.util.Collections.singletonList(
                DynamoDbAttributeValueDto.fromPlainValue("profile", reversed))));
    }
}
