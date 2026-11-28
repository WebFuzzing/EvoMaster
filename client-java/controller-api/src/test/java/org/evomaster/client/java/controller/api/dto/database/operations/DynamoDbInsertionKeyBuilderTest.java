package org.evomaster.client.java.controller.api.dto.database.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("country", "Argentina");
        String first = DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("profile", player)));
        Map<String, Object> reversed = new LinkedHashMap<>();
        reversed.put("country", "Argentina");
        reversed.put("name", "Lionel Messi");
        assertEquals(first, DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("profile", reversed))));
        reversed.put("name", "Angel Di Maria");
        assertNotEquals(first, DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("profile", reversed))));
    }

    @Test
    public void testSetIdentityIsOrderIndependentAndLengthDelimited() {
        String first = DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("tags", new LinkedHashSet<>(Arrays.asList("b", "a")))));
        String reordered = DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("tags", new LinkedHashSet<>(Arrays.asList("a", "b")))));
        assertEquals(first, reordered);

        String commaLeft = DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("tags", new LinkedHashSet<>(Arrays.asList("a,b", "c")))));
        String commaRight = DynamoDbInsertionKeyBuilder.fromAttributes("players", Collections.singletonList(
                DynamoDbDtoUtils.fromPlainValue("tags", new LinkedHashSet<>(Arrays.asList("a", "b,c")))));
        assertNotEquals(commaLeft, commaRight);
    }
}
