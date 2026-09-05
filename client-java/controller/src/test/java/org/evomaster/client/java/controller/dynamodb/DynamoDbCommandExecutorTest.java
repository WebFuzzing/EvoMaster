package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests DynamoDB item insertion through synchronous and asynchronous AWS clients. */
public class DynamoDbCommandExecutorTest {

    @Test
    public void testExecuteInsertWithSynchronousClient() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        when(client.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        DynamoDbInsertionResultsDto results = DynamoDbCommandExecutor.executeInsert(
                client, Collections.singletonList(worldCupPlayer()));

        ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(client).putItem(requestCaptor.capture());
        PutItemRequest request = requestCaptor.getValue();
        assertEquals("WorldCupPlayers", request.tableName());
        assertEquals("Argentina", request.item().get("country").s());
        assertEquals("10", request.item().get("fifaId").n());
        assertTrue(request.item().get("captain").bool());
        assertEquals(Collections.singletonList(true), results.executionResults);
        assertNull(results.failedInsertionIndex);
    }

    @Test
    public void testExecuteInsertWithAsynchronousClient() {
        DynamoDbAsyncClient client = mock(DynamoDbAsyncClient.class);
        when(client.putItem(any(PutItemRequest.class))).thenReturn(
                CompletableFuture.completedFuture(PutItemResponse.builder().build()));

        DynamoDbInsertionResultsDto results = DynamoDbCommandExecutor.executeInsert(
                client, Collections.singletonList(worldCupPlayer()));

        verify(client).putItem(any(PutItemRequest.class));
        assertEquals(Collections.singletonList(true), results.executionResults);
    }

    @Test
    public void testFailureContainsPartialResults() {
        DynamoDbClient client = mock(DynamoDbClient.class);
        when(client.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build())
                .thenThrow(new IllegalStateException("DynamoDB unavailable"));

        DynamoDbCommandExecutor.DynamoDbInsertionException error = assertThrows(
                DynamoDbCommandExecutor.DynamoDbInsertionException.class,
                () -> DynamoDbCommandExecutor.executeInsert(
                        client, Arrays.asList(worldCupPlayer(), worldCupPlayer())));

        assertEquals(1, error.getFailedIndex());
        assertEquals(Arrays.asList(true, false), error.getResults().executionResults);
        assertEquals(Integer.valueOf(1), error.getResults().failedInsertionIndex);
        verify(client, times(2)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void testRejectsMissingClientOrInsertions() {
        DynamoDbClient client = mock(DynamoDbClient.class);

        assertThrows(IllegalArgumentException.class,
                () -> DynamoDbCommandExecutor.executeInsert(null, Collections.singletonList(worldCupPlayer())));
        assertThrows(IllegalArgumentException.class,
                () -> DynamoDbCommandExecutor.executeInsert(client, null));
        assertThrows(IllegalArgumentException.class,
                () -> DynamoDbCommandExecutor.executeInsert(client, Collections.emptyList()));
        verify(client, times(0)).putItem(any(PutItemRequest.class));
    }

    private DynamoDbInsertionDto worldCupPlayer() {
        DynamoDbInsertionDto insertion = new DynamoDbInsertionDto();
        insertion.tableName = "WorldCupPlayers";
        insertion.attributes.add(new DynamoDbAttributeValueDto("country", DynamoDbScalarTypeDto.STRING, "Argentina"));
        insertion.attributes.add(new DynamoDbAttributeValueDto("fifaId", DynamoDbScalarTypeDto.NUMBER, "10"));
        insertion.attributes.add(new DynamoDbAttributeValueDto("captain", DynamoDbScalarTypeDto.BOOLEAN, "true"));
        return insertion;
    }
}
