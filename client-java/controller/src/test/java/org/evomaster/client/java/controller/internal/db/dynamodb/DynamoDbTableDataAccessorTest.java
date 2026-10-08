package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests reflective DynamoDB table access independently of command heuristic processing.
 */
public class DynamoDbTableDataAccessorTest {

    private static final String TABLE = "WorldCupPlayers";

    private final DynamoDbTableDataAccessor accessor = new DynamoDbTableDataAccessor();

    @Test
    public void testReadsAndNormalizesAllSynchronousPages() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        Map<String, AttributeValue> nextKey = Collections.singletonMap(
                "country", AttributeValue.builder().s("Argentina").build());
        ScanResponse firstPage = ScanResponse.builder()
                .items(Collections.singletonList(player("Diego Maradona", 190042L)))
                .lastEvaluatedKey(nextKey)
                .build();
        ScanResponse secondPage = ScanResponse.builder()
                .items(Collections.singletonList(player("Lionel Messi", 158023L)))
                .lastEvaluatedKey(Collections.emptyMap())
                .build();
        when(client.scan(any(ScanRequest.class))).thenReturn(firstPage, secondPage);

        List<Map<String, Object>> items = accessor.getItems(client, TABLE);

        assertEquals(2, items.size());
        assertEquals("Diego Maradona", items.get(0).get("playerName"));
        assertEquals(190042L, items.get(0).get("fifaId"));
        assertEquals("Lionel Messi", items.get(1).get("playerName"));
        assertEquals(158023L, items.get(1).get("fifaId"));

        ArgumentCaptor<ScanRequest> requests = ArgumentCaptor.forClass(ScanRequest.class);
        verify(client, times(2)).scan(requests.capture());
        assertEquals(Arrays.asList(TABLE, TABLE), Arrays.asList(
                requests.getAllValues().get(0).tableName(),
                requests.getAllValues().get(1).tableName()));
        assertTrue(requests.getAllValues().get(0).exclusiveStartKey().isEmpty());
        assertEquals(nextKey, requests.getAllValues().get(1).exclusiveStartKey());
    }

    @Test
    public void testAwaitsAsynchronousResponse() {
        DynamoDbAsyncClient client = mock(DynamoDbAsyncClient.class);
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(player("Lionel Messi", 158023L)))
                .build();
        when(client.scan(any(ScanRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        List<Map<String, Object>> items = accessor.getItems(client, TABLE);

        assertEquals(1, items.size());
        assertEquals("Argentina", items.get(0).get("country"));
        assertEquals("Lionel Messi", items.get(0).get("playerName"));
        verify(client).scan(any(ScanRequest.class));
    }

    @Test
    public void testRejectsRepeatedPaginationKey() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        Map<String, AttributeValue> nextKey = Collections.singletonMap(
                "country", AttributeValue.builder().s("Argentina").build());
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(player("Lionel Messi", 158023L)))
                .lastEvaluatedKey(nextKey)
                .build();
        when(client.scan(any(ScanRequest.class))).thenReturn(response);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> accessor.getItems(client, TABLE));

        assertEquals("DynamoDB scan returned the same pagination key twice", exception.getMessage());
        verify(client, times(2)).scan(any(ScanRequest.class));
    }

    @Test
    public void testReportsUnderlyingScanFailure() {
        IllegalStateException scanFailure;
        RuntimeException exception;
        try (DynamoDbClient client = mock(DynamoDbClient.class)) {
            scanFailure = new IllegalStateException("World Cup table unavailable");
            when(client.scan(any(ScanRequest.class))).thenThrow(scanFailure);

            exception = assertThrows(RuntimeException.class,
                    () -> accessor.getItems(client, TABLE));
        }

        assertSame(scanFailure, exception.getCause());
        assertTrue(exception.getMessage().contains(TABLE));
        assertTrue(exception.getMessage().contains("World Cup table unavailable"));
    }

    /**
     * Verifies required accessor arguments are validated before reflection.
     */
    @Test
    public void testRejectsNullArguments() {
        NullPointerException nullClient = assertThrows(NullPointerException.class,
                () -> accessor.getItems(null, TABLE));
        assertEquals("DynamoDB client cannot be null", nullClient.getMessage());

        NullPointerException nullTable = assertThrows(NullPointerException.class,
                () -> accessor.getItems(mock(DynamoDbClient.class), null));
        assertEquals("DynamoDB table name cannot be null", nullTable.getMessage());
    }

    /**
     * Verifies unsupported client objects fail with a clear contract error.
     */
    @Test
    public void testRejectsClientWithoutScanMethod() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accessor.getItems(new Object(), TABLE));

        assertEquals("The provided client does not expose scan(ScanRequest)", exception.getMessage());
    }

    /**
     * Creates one deterministic World Cup player item.
     *
     * @param playerName player name
     * @param fifaId FIFA identifier
     * @return DynamoDB item
     */
    private Map<String, AttributeValue> player(String playerName, long fifaId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("country", AttributeValue.builder().s("Argentina").build());
        item.put("playerName", AttributeValue.builder().s(playerName).build());
        item.put("fifaId", AttributeValue.builder().n(Long.toString(fifaId)).build());
        return item;
    }
}
