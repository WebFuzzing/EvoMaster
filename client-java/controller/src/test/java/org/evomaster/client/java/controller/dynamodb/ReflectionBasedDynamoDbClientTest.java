package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Checks that {@link ReflectionBasedDynamoDbClient} finds and calls the DynamoDB client's
 * methods through reflection without compile-time coupling to concrete client types.
 */
public class ReflectionBasedDynamoDbClientTest {

    private static final String TABLE = "WorldCupPlayers";

    @Test
    public void testRejectsNullClient() {
        assertThrows(NullPointerException.class, () -> new ReflectionBasedDynamoDbClient(null));
    }

    @Test
    public void testExecuteInsertionsSynchronousClient() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        when(mockClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(worldCupPlayer()));

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockClient).putItem(captor.capture());
        PutItemRequest request = captor.getValue();
        assertEquals(TABLE, request.tableName());
        assertEquals("Argentina", request.item().get("country").s());
        assertEquals("10", request.item().get("fifaId").n());
        assertTrue(request.item().get("captain").bool());
        assertEquals(Collections.singletonList(true), results.executionResults);
        assertNull(results.failedInsertionIndex);
    }

    @Test
    public void testExecuteInsertionsAsynchronousClient() {
        DynamoDbAsyncClient mockClient = mock(DynamoDbAsyncClient.class);
        when(mockClient.putItem(any(PutItemRequest.class))).thenReturn(
                CompletableFuture.completedFuture(PutItemResponse.builder().build()));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(worldCupPlayer()));

        verify(mockClient).putItem(any(PutItemRequest.class));
        assertEquals(Collections.singletonList(true), results.executionResults);
    }

    @Test
    public void testExecuteInsertionsPartialFailure() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        when(mockClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build())
                .thenThrow(new IllegalStateException("DynamoDB service error"));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(
                Arrays.asList(worldCupPlayer(), worldCupPlayer(), worldCupPlayer()));

        assertEquals(Arrays.asList(true, false, false), results.executionResults);
        assertEquals(Integer.valueOf(1), results.failedInsertionIndex);
        verify(mockClient, times(2)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void testExecuteInsertionsRejectsMissingInsertions() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);

        assertThrows(NullPointerException.class, () -> client.executeInsertions(null));
        assertThrows(IllegalArgumentException.class, () -> client.executeInsertions(Collections.emptyList()));
        verify(mockClient, times(0)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void testExecuteInsertionsRejectsClientWithoutPutItemMethod() {
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(new Object());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.executeOne(worldCupPlayer()));
        assertEquals("The provided client does not expose putItem(PutItemRequest)", exception.getMessage());

        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(worldCupPlayer()));
        assertEquals(Collections.singletonList(false), results.executionResults);
        assertEquals(Integer.valueOf(0), results.failedInsertionIndex);
    }

    @Test
    public void testGetItemsWithPagination() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        Map<String, AttributeValue> nextKey = Collections.singletonMap(
                "country", AttributeValue.builder().s("Argentina").build());

        Map<String, AttributeValue> item1 = new LinkedHashMap<>();
        item1.put("playerName", AttributeValue.builder().s("Diego Maradona").build());
        item1.put("fifaId", AttributeValue.builder().n("190042").build());

        Map<String, AttributeValue> item2 = new LinkedHashMap<>();
        item2.put("playerName", AttributeValue.builder().s("Lionel Messi").build());
        item2.put("fifaId", AttributeValue.builder().n("158023").build());

        ScanResponse page1 = ScanResponse.builder()
                .items(Collections.singletonList(item1))
                .lastEvaluatedKey(nextKey)
                .build();
        ScanResponse page2 = ScanResponse.builder()
                .items(Collections.singletonList(item2))
                .lastEvaluatedKey(Collections.emptyMap())
                .build();

        when(mockClient.scan(any(ScanRequest.class))).thenReturn(page1, page2);

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        List<Map<String, Object>> items = client.getItems(TABLE);

        assertEquals(2, items.size());
        assertEquals("Diego Maradona", items.get(0).get("playerName"));
        assertEquals("Lionel Messi", items.get(1).get("playerName"));
        verify(mockClient, times(2)).scan(any(ScanRequest.class));
    }

    @Test
    public void testGetItemsAsynchronousClient() {
        DynamoDbAsyncClient mockClient = mock(DynamoDbAsyncClient.class);
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("country", AttributeValue.builder().s("Argentina").build());
        item.put("playerName", AttributeValue.builder().s("Lionel Messi").build());

        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(item))
                .build();
        when(mockClient.scan(any(ScanRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        List<Map<String, Object>> items = client.getItems(TABLE);

        assertEquals(1, items.size());
        assertEquals("Argentina", items.get(0).get("country"));
        assertEquals("Lionel Messi", items.get(0).get("playerName"));
        verify(mockClient).scan(any(ScanRequest.class));
    }

    @Test
    public void testGetItemsRejectsRepeatedPaginationKey() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        Map<String, AttributeValue> nextKey = Collections.singletonMap(
                "country", AttributeValue.builder().s("Argentina").build());
        Map<String, AttributeValue> item = Collections.singletonMap(
                "playerName", AttributeValue.builder().s("Lionel Messi").build());
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(item))
                .lastEvaluatedKey(nextKey)
                .build();
        when(mockClient.scan(any(ScanRequest.class))).thenReturn(response);

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.getItems(TABLE));

        assertEquals("DynamoDB scan returned the same pagination key twice", exception.getMessage());
        verify(mockClient, times(2)).scan(any(ScanRequest.class));
    }

    @Test
    public void testGetItemsReportsUnderlyingScanFailure() {
        IllegalStateException scanFailure;
        RuntimeException exception;
        try (DynamoDbClient mockClient = mock(DynamoDbClient.class)) {
            scanFailure = new IllegalStateException("World Cup table unavailable");
            when(mockClient.scan(any(ScanRequest.class))).thenThrow(scanFailure);

            ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
            exception = assertThrows(RuntimeException.class,
                    () -> client.getItems(TABLE));
        }

        assertSame(scanFailure, exception.getCause());
        assertTrue(exception.getMessage().contains(TABLE));
        assertTrue(exception.getMessage().contains("World Cup table unavailable"));
    }

    @Test
    public void testGetItemsRejectsNullTableName() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);

        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> client.getItems(null));
        assertEquals("DynamoDB table name cannot be null", exception.getMessage());
    }

    @Test
    public void testGetItemsRejectsClientWithoutScanMethod() {
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(new Object());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.getItems(TABLE));

        assertEquals("The provided client does not expose scan(ScanRequest)", exception.getMessage());
    }

    private static DynamoDbInsertionDto worldCupPlayer() {
        DynamoDbInsertionDto dto = new DynamoDbInsertionDto();
        dto.tableName = TABLE;
        dto.attributes = new ArrayList<>();
        dto.attributes.add(new DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.STRING, "Argentina"));
        dto.attributes.add(new DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.NUMBER, "10"));
        dto.attributes.add(new DynamoDbAttributeValueDto("captain", DynamoDbScalarTypeDto.BOOLEAN, "true"));
        return dto;
    }
}
