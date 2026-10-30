package org.evomaster.client.java.instrumentation.dynamodb;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
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
            DynamoDbQuery query = roundTripped.getDynamoDbInfoData().iterator().next().getDdbRequest();
            assertEquals("FIFAWorldCupPlayers", query.tableName());
            assertEquals("#id = :id", query.filterExpression());
            assertEquals(names, query.expressionAttributeNames());
            DynamoDbQueryValue id = query.expressionAttributeValues().get(":id");
            assertEquals(DynamoDbAttributeType.BINARY, id.type());
            assertArrayEquals(new byte[]{10, 7, 86}, (byte[]) id.getPayload());
        }
    }

    /** Verifies that a direct read retains its composite key types and values. */
    @Test
    public void testGetItemCapturesCompositeKey() {
        Map<String, AttributeValue> key = new LinkedHashMap<>();
        key.put("playerId", AttributeValue.builder().s("Messi").build());
        key.put("seasonYear", AttributeValue.builder().n("2022").build());

        DynamoDbQuery query = DynamoDbQuery.from(GetItemRequest.builder()
                .tableName("WorldCupPlayers")
                .key(key)
                .build());

        assertEquals(DynamoDbAttributeType.STRING, query.key().get("playerId").type());
        assertEquals("Messi", query.key().get("playerId").getPayload());
        assertEquals(DynamoDbAttributeType.NUMBER, query.key().get("seasonYear").type());
        assertEquals("2022", query.key().get("seasonYear").getPayload());
    }

    /** An absent attribute type differs from an explicit DynamoDB null value. */
    @Test
    public void testGetItemDistinguishesUnsetAndExplicitNullKeyValues() {
        Map<String, AttributeValue> key = new LinkedHashMap<>();
        key.put("missingPlayer", AttributeValue.builder().build());
        key.put("retiredPlayer", AttributeValue.builder().nul(true).build());

        DynamoDbQuery query = DynamoDbQuery.from(GetItemRequest.builder()
                .tableName("WorldCupPlayers")
                .key(key)
                .build());

        assertEquals(DynamoDbAttributeType.UNSET, query.key().get("missingPlayer").type());
        assertEquals(DynamoDbAttributeType.NULL, query.key().get("retiredPlayer").type());
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

        DynamoDbQuery query = DynamoDbQuery.from(BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build());
        Map<String, DynamoDbQuery.BatchGetKeys> captured = query.batchRequestItems();

        assertEquals(requestItems.keySet(), captured.keySet());
        assertEquals("Messi", captured.get("WorldCupPlayers").keys().get(0).get("playerId").getPayload());
        DynamoDbQueryValue teamId = captured.get("WorldCupTeams").keys().get(0).get("teamId");
        assertEquals(DynamoDbAttributeType.NUMBER, teamId.type());
        assertEquals("10", teamId.getPayload());
    }
}
