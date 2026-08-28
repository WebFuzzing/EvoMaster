package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the DynamoDB command-to-heuristic integration.
 */
public class DynamoDbHandlerTest {

    private static final String TABLE = "WorldCupPlayers";

     @Test
    public void testSyncClientPaginationAndTableReuse() {
        SyncDynamoDbClient client = new SyncDynamoDbClient();
        DynamoDbHandler handler = enabledHandler(client);
        DynamoDbCommand command = queryCommand("Lionel Messi");

        handler.handle(command);
        handler.handle(command);
        List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

        assertEquals(2, evaluated.size());
        assertEquals(2, client.scanCalls);
        for (DynamoDbCommandWithDistance result : evaluated) {
            assertEquals(0.0d, result.getDistanceWithMetrics().getDistance(), 0.0d);
            assertEquals(2, result.getDistanceWithMetrics().getNumberOfEvaluatedItems());
            assertFalse(result.getDistanceWithMetrics().isEvaluationFailure());
        }
    }

    @Test
    public void testAsyncClient() {
        AsyncDynamoDbClient client = new AsyncDynamoDbClient();
        DynamoDbHandler handler = enabledHandler(client);
        handler.handle(queryCommand("Lionel Scaloni"));

        List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, evaluated.size());
        assertTrue(evaluated.get(0).getDistanceWithMetrics().getDistance() > 0.0d);
        assertTrue(evaluated.get(0).getDistanceWithMetrics().getDistance() < 1.0d);
        assertEquals(1, evaluated.get(0).getDistanceWithMetrics().getNumberOfEvaluatedItems());
    }

    @Test
    public void testPrivateClientImplementation() {
        DynamoDbHandler handler = enabledHandler(new PrivateAsyncDynamoDbClient());
        handler.handle(queryCommand("Lionel Scaloni"));

        List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, evaluated.size());
        assertFalse(evaluated.get(0).getDistanceWithMetrics().isEvaluationFailure());
        assertEquals(1, evaluated.get(0).getDistanceWithMetrics().getNumberOfEvaluatedItems());
    }

    @Test
    public void testFailureIsReported() {
        DynamoDbHandler handler = enabledHandler(new Object());
        handler.handle(queryCommand("Diego Maradona"));

        List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, evaluated.size());
        assertEquals(1.0d, evaluated.get(0).getDistanceWithMetrics().getDistance(), 0.0d);
        assertTrue(evaluated.get(0).getDistanceWithMetrics().isEvaluationFailure());
    }

    @Test
    public void testDisabledAndReset() {
        DynamoDbHandler handler = new DynamoDbHandler();
        handler.setDynamoDbClient(new SyncDynamoDbClient());
        handler.handle(queryCommand("Lionel Messi"));
        assertFalse(handler.getEvaluatedDynamoDbCommands().isEmpty());

        handler.reset();
        handler.setCalculateHeuristics(false);
        handler.handle(queryCommand("Lionel Messi"));
        assertTrue(handler.getEvaluatedDynamoDbCommands().isEmpty());

        handler.setCalculateHeuristics(true);
        handler.handle(queryCommand("Lionel Messi"));
        handler.reset();
        assertTrue(handler.getEvaluatedDynamoDbCommands().isEmpty());
    }

    private DynamoDbHandler enabledHandler(Object client) {
        DynamoDbHandler handler = new DynamoDbHandler();
        handler.setCalculateHeuristics(true);
        handler.setDynamoDbClient(client);
        return handler;
    }

    private DynamoDbCommand queryCommand(String playerName) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":country", AttributeValue.builder().s("Argentina").build());
        values.put(":player", AttributeValue.builder().s(playerName).build());
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression("country = :country")
                .filterExpression("playerName = :player")
                .expressionAttributeValues(values)
                .build();
        return new DynamoDbCommand(Collections.singletonList(TABLE), DynamoDbOperationNames.QUERY,
                request, true, 1L);
    }

    private static Map<String, AttributeValue> item(String country, String playerName) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("country", AttributeValue.builder().s(country).build());
        item.put("playerName", AttributeValue.builder().s(playerName).build());
        return item;
    }

    /**
     * Minimal synchronous client used to exercise reflection without mocking.
     */
    public static final class SyncDynamoDbClient {

        private int scanCalls;

        /**
         * Returns two deterministic pages of World Cup player data.
         *
         * @param request scan request
         * @return scan response page
         */
        public ScanResponse scan(ScanRequest request) {
            scanCalls++;
            if (request.exclusiveStartKey().isEmpty()) {
                Map<String, AttributeValue> nextKey = Collections.singletonMap(
                        "country", AttributeValue.builder().s("Argentina").build());
                return ScanResponse.builder()
                        .items(Collections.singletonList(item("Argentina", "Diego Maradona")))
                        .lastEvaluatedKey(nextKey)
                        .build();
            }
            return ScanResponse.builder()
                    .items(Collections.singletonList(item("Argentina", "Lionel Messi")))
                    .lastEvaluatedKey(Collections.<String, AttributeValue>emptyMap())
                    .build();
        }
    }

    /**
     * Minimal asynchronous client used to exercise completion-stage handling.
     */
    public static final class AsyncDynamoDbClient {

        /**
         * Returns one asynchronous page of World Cup player data.
         *
         * @param request scan request
         * @return completed asynchronous scan response
         */
        public CompletableFuture<ScanResponse> scan(ScanRequest request) {
            List<Map<String, AttributeValue>> items = new ArrayList<>();
            items.add(item("Argentina", "Lionel Messi"));
            ScanResponse response = ScanResponse.builder().items(items).build();
            return CompletableFuture.completedFuture(response);
        }
    }

    /**
     * Public scan contract implemented by a non-public SDK-like client class.
     */
    public interface AsyncScanClient {

        /**
         * Scans one table page.
         *
         * @param request scan request
         * @return completed scan response
         */
        CompletableFuture<ScanResponse> scan(ScanRequest request);
    }

    /**
     * Mimics the AWS SDK pattern of returning a package-private implementation of a public client.
     */
    private static final class PrivateAsyncDynamoDbClient implements AsyncScanClient {

        /**
         * Returns one asynchronous World Cup player page.
         *
         * @param request scan request
         * @return completed scan response
         */
        @Override
        public CompletableFuture<ScanResponse> scan(ScanRequest request) {
            ScanResponse response = ScanResponse.builder()
                    .items(Collections.singletonList(item("Argentina", "Lionel Messi")))
                    .build();
            return CompletableFuture.completedFuture(response);
        }
    }
}
