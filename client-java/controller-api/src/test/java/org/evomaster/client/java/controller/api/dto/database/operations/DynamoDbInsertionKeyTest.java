package org.evomaster.client.java.controller.api.dto.database.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the canonical representation of inferred DynamoDB insertion keys.
 */
public class DynamoDbInsertionKeyTest {

    @Test
    public void testBuildsWorldCupPlayerInsertionKey() {
        assertEquals("WorldCupPlayers|country:STRING=Argentina|fifaId:NUMBER=10|captain:BOOLEAN=true",
                DynamoDbInsertionKey.fromAttributes("WorldCupPlayers", Arrays.asList(
                        new DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.STRING, "Argentina"),
                        new DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.NUMBER, "10"),
                        new DynamoDbAttributeValueDto("captain", DynamoDbScalarTypeDto.BOOLEAN, "true")
                )));
    }
}
