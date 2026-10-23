package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests DynamoDB table item caching and validation invariants.
 */
public class DynamoDbTableItemsCacheTest {

    private static final String TABLE = "WorldCupPlayers";

    private final DynamoDbTableDataAccessor accessor = new DynamoDbTableDataAccessor();

    /**
     * Verifies that items for a table are loaded on the first call and cached for subsequent calls.
     */
    @Test
    public void testCachesTableItemsAcrossMultipleRequests() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(player("Argentina", "Lionel Messi", 10)))
                .build();
        when(client.scan(any(ScanRequest.class))).thenReturn(response);

        DynamoDbTableItemsCache cache = new DynamoDbTableItemsCache(accessor, client);

        List<Map<String, Object>> first = cache.getItems(TABLE);
        List<Map<String, Object>> second = cache.getItems(TABLE);

        assertEquals(1, first.size());
        assertEquals("Lionel Messi", first.get(0).get("playerName"));
        assertEquals(first, second);
        verify(client, times(1)).scan(any(ScanRequest.class));
    }

    /**
     * Verifies that whitespace-padded table names are trimmed and resolve to the same cache entry.
     */
    @Test
    public void testTrimsTableNameAndReusesCachedItems() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(player("Argentina", "Diego Maradona", 10)))
                .build();
        when(client.scan(any(ScanRequest.class))).thenReturn(response);

        DynamoDbTableItemsCache cache = new DynamoDbTableItemsCache(accessor, client);

        List<Map<String, Object>> direct = cache.getItems(TABLE);
        List<Map<String, Object>> padded = cache.getItems("  " + TABLE + "  ");

        assertEquals(direct, padded);
        verify(client, times(1)).scan(any(ScanRequest.class));
    }

    /**
     * Verifies that null and blank table names are rejected.
     */
    @Test
    public void testRejectsNullOrBlankTableName() {
        DynamoDbTableItemsCache cache = new DynamoDbTableItemsCache(accessor, mock(DynamoDbClient.class));

        NullPointerException nullTable = assertThrows(NullPointerException.class, () -> cache.getItems(null));
        assertEquals("DynamoDB table name cannot be null", nullTable.getMessage());

        IllegalArgumentException emptyTable = assertThrows(IllegalArgumentException.class, () -> cache.getItems(""));
        assertEquals("DynamoDB table name cannot be blank", emptyTable.getMessage());

        IllegalArgumentException blankTable = assertThrows(IllegalArgumentException.class, () -> cache.getItems("   "));
        assertEquals("DynamoDB table name cannot be blank", blankTable.getMessage());
    }

    /**
     * Verifies that null accessor arguments in constructor are rejected.
     */
    @Test
    public void testRejectsNullConstructorArguments() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new DynamoDbTableItemsCache(null, new Object()));
        assertEquals("DynamoDB table data accessor cannot be null", exception.getMessage());
    }

    /**
     * Verifies that cached item lists and maps cannot be mutated by callers.
     */
    @Test
    public void testReturnsUnmodifiableListAndMaps() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(player("Croatia", "Luka Modric", 10)))
                .build();
        when(client.scan(any(ScanRequest.class))).thenReturn(response);

        DynamoDbTableItemsCache cache = new DynamoDbTableItemsCache(accessor, client);
        List<Map<String, Object>> items = cache.getItems(TABLE);

        Map<String, Object> newItem = new HashMap<>();
        newItem.put("country", "France");
        newItem.put("playerName", "Kylian Mbappe");
        assertThrows(UnsupportedOperationException.class, () -> items.add(newItem));
        assertThrows(UnsupportedOperationException.class, () -> items.get(0).put("goals", 5));
    }

    /**
     * Creates one deterministic World Cup player item representation.
     *
     * @param country country name
     * @param playerName player name
     * @param squadNumber player squad number
     * @return map representing player attributes as AttributeValues
     */
    private Map<String, AttributeValue> player(String country, String playerName, int squadNumber) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("country", AttributeValue.builder().s(country).build());
        item.put("playerName", AttributeValue.builder().s(playerName).build());
        item.put("squadNumber", AttributeValue.builder().n(Integer.toString(squadNumber)).build());
        return item;
    }
}
