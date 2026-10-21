package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeType;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies transport and key capture for serializable DynamoDB requests.
 */
public class DynamoDbCommandSerializationTest {

    /** Verifies that an intercepted write request survives AdditionalInfo serialization. */
    @Test
    public void testDynamoDbCommandCanBeSerializedForAdditionalInfoTransport() throws Exception {
        Map<String, String> names = Collections.singletonMap("#id", "id");
        PutItemRequest request = PutItemRequest.builder()
                .tableName("FIFAWorldCupPlayers")
                .conditionExpression("#id = :id")
                .expressionAttributeNames(names)
                .expressionAttributeValues(Collections.singletonMap(
                        ":id", AttributeValue.builder().b(SdkBytes.fromByteArray(new byte[]{10, 7, 86})).build()))
                .build();
        DynamoDbCommand command = new DynamoDbCommand(
                Collections.singletonList("FIFAWorldCupPlayers"),
                DynamoDbOperationNames.PUT_ITEM,
                request,
                true,
                1L);
        AdditionalInfo additionalInfo = new AdditionalInfo();
        additionalInfo.addDynamoDbInfo(command);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(additionalInfo);
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            AdditionalInfo roundTripped = (AdditionalInfo) input.readObject();
            DynamoDbRequestSnapshot snapshot = roundTripped.getDynamoDbInfoData().iterator().next().getDdbRequest();
            assertEquals("FIFAWorldCupPlayers", snapshot.tableName());
            assertEquals("#id = :id", snapshot.filterExpression());
            assertEquals(names, snapshot.expressionAttributeNames());
            DynamoDbValueSnapshot id = snapshot.expressionAttributeValues().get(":id");
            assertEquals(DynamoDbAttributeType.BINARY, id.type());
            assertArrayEquals(new byte[]{10, 7, 86}, (byte[]) id.value());
        }
    }

    /** Verifies that a direct read retains its composite key types and values. */
    @Test
    public void testGetItemCapturesCompositeKey() {
        Map<String, AttributeValue> key = new LinkedHashMap<>();
        key.put("playerId", AttributeValue.builder().s("Messi").build());
        key.put("seasonYear", AttributeValue.builder().n("2022").build());

        DynamoDbRequestSnapshot snapshot = DynamoDbRequestSnapshot.from(GetItemRequest.builder()
                .tableName("WorldCupPlayers")
                .key(key)
                .build());

        assertEquals(DynamoDbAttributeType.STRING, snapshot.key().get("playerId").type());
        assertEquals("Messi", snapshot.key().get("playerId").value());
        assertEquals(DynamoDbAttributeType.NUMBER, snapshot.key().get("seasonYear").type());
        assertEquals("2022", snapshot.key().get("seasonYear").value());
    }

    /** Verifies that a batch read retains keys for each requested table. */
    @Test
    public void testBatchGetCapturesKeysByTable() {
        Map<String, KeysAndAttributes> requestItems = new LinkedHashMap<>();
        requestItems.put("WorldCupPlayers", KeysAndAttributes.builder()
                .keys(Collections.singletonMap("playerId", AttributeValue.builder().s("Messi").build()))
                .build());
        requestItems.put("WorldCupTeams", KeysAndAttributes.builder()
                .keys(Collections.singletonMap("teamId", AttributeValue.builder().n("10").build()))
                .build());

        DynamoDbRequestSnapshot snapshot = DynamoDbRequestSnapshot.from(BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build());
        Map<String, DynamoDbRequestSnapshot.KeysAndAttributesSnapshot> captured = snapshot.batchRequestItems();

        assertEquals(requestItems.keySet(), captured.keySet());
        assertEquals("Messi", captured.get("WorldCupPlayers").keys().get(0).get("playerId").value());
        DynamoDbValueSnapshot teamId = captured.get("WorldCupTeams").keys().get(0).get("teamId");
        assertEquals(DynamoDbAttributeType.NUMBER, teamId.type());
        assertEquals("10", teamId.value());
    }
}
