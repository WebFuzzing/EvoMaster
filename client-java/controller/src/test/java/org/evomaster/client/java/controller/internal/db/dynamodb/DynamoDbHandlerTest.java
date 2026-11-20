package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

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

    /** Equivalent query predicates produce one diagnostic ID despite different player values. */
    @Test
    public void testHeuristicIdGroupsEquivalentQueries() {
        DynamoDbDistanceWithMetrics distance = new DynamoDbDistanceWithMetrics(0.0d, 0, false);
        String messiId = new DynamoDbCommandWithDistance(queryCommand("Lionel Messi"), TABLE, distance)
                .getHeuristicId();
        String maradonaId = new DynamoDbCommandWithDistance(queryCommand("Diego Maradona"), TABLE, distance)
                .getHeuristicId();

        assertEquals(messiId, maradonaId);
        assertTrue(messiId.startsWith("Query:" + TABLE + ":"));
        assertFalse(messiId.contains("Lionel Messi"));
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
        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testFailedConditionalPutItemIsEvaluated() {
        SyncDynamoDbClient client = new SyncDynamoDbClient();
        DynamoDbHandler handler = enabledHandler(client);
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":player", AttributeValue.builder().s("Lionel Scaloni").build());
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE)
                .item(item("Argentina", "Lionel Messi"))
                .conditionExpression("playerName = :player")
                .expressionAttributeValues(values)
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList(TABLE), DynamoDbOperationNames.PUT_ITEM,
                request, false, 1L));

        List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, evaluated.size());
        assertTrue(evaluated.get(0).getDistanceWithMetrics().getDistance() > 0.0d);
        assertTrue(evaluated.get(0).getDistanceWithMetrics().getDistance() < 1.0d);
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

    @Test
    public void testExtractsAndDeduplicatesNestedWorldCupPlayerQueries() {
        SyncDynamoDbClient client = new SyncDynamoDbClient();
        DynamoDbHandler handler = enabledHandler(client);
        handler.handle(nestedQueryCommand());
        handler.handle(nestedQueryCommand());

        handler.getEvaluatedDynamoDbCommands();
        DynamoDbExecutionsDto execution = handler.getExecutionDto();

        assertEquals(1, execution.failedQueries.size());
        DynamoDbFailedQuery failedQuery = execution.failedQueries.get(0);
        assertEquals(TABLE, failedQuery.tableName);
        assertAttribute(failedQuery.attributes, "country", DynamoDbScalarTypeDto.STRING, "Argentina");
        assertAttribute(failedQuery.attributes, "playerName", DynamoDbScalarTypeDto.STRING, "Lionel Scaloni");
        assertAttribute(failedQuery.attributes, "fifaId", DynamoDbScalarTypeDto.NUMBER, "10");
        assertAttribute(failedQuery.attributes, "captain", DynamoDbScalarTypeDto.BOOLEAN, "true");
        assertEquals(1, client.describeCalls);

        handler.reset();
        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testExtractionCanRunWithoutHeuristicCollection() {
        DynamoDbHandler handler = enabledHandler(new SyncDynamoDbClient());
        handler.setCalculateHeuristics(false);
        handler.handle(queryCommand("Lionel Scaloni"));

        assertTrue(handler.getEvaluatedDynamoDbCommands().isEmpty());
        assertEquals(1, handler.getExecutionDto().failedQueries.size());

        handler.reset();
        handler.setExtractDynamoDbExecution(false);
        handler.handle(queryCommand("Lionel Scaloni"));
        handler.getEvaluatedDynamoDbCommands();
        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testReportsMissingPrimaryKeyForPartitionQuery() {
        DynamoDbHandler handler = enabledHandler(new SyncDynamoDbClient());
        QueryRequest request = QueryRequest.builder()
                .tableName("records")
                .keyConditionExpression("subject_id = :subject")
                .expressionAttributeValues(Collections.singletonMap(":subject", AttributeValue.builder().s("England").build()))
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList("records"), DynamoDbOperationNames.QUERY,
                request, true, 1L));

        handler.getEvaluatedDynamoDbCommands();
        List<DynamoDbAttributeValueDto> attributes = handler.getExecutionDto().failedQueries.get(0).attributes;

        assertAttribute(attributes, "subject_id", DynamoDbScalarTypeDto.STRING, "England");
        assertFalse(attributes.stream().anyMatch(attribute -> "record_key".equals(attribute.attributeName)));
        assertEquals(Collections.singletonMap("record_key", DynamoDbScalarTypeDto.STRING),
                handler.getExecutionDto().failedQueries.get(0).missingPrimaryKeyAttributes);
    }

    @Test
    public void testInferredRecordRetainsNonKeyScalarAttributes() {
        DynamoDbHandler handler = enabledHandler(new SyncDynamoDbClient());
        QueryRequest request = QueryRequest.builder()
                .tableName("records")
                .keyConditionExpression("subject_id = :subject")
                .expressionAttributeValues(Collections.singletonMap(":subject", AttributeValue.builder().s("Brazil").build()))
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList("records"), DynamoDbOperationNames.QUERY,
                request, true, 1L));

        handler.getEvaluatedDynamoDbCommands();
        List<DynamoDbAttributeValueDto> attributes = handler.getExecutionDto().failedQueries.get(0).attributes;

        assertAttribute(attributes, "subject_id", DynamoDbScalarTypeDto.STRING, "Brazil");
        assertAttribute(attributes, "purpose", DynamoDbScalarTypeDto.STRING, "World Cup tickets");
        assertAttribute(attributes, "version", DynamoDbScalarTypeDto.NUMBER, "1");
        assertAttribute(attributes, "tombstoned", DynamoDbScalarTypeDto.BOOLEAN, "false");
        assertFalse(attributes.stream().anyMatch(attribute -> "final-ticket".equals(attribute.value)));
    }

    @Test
    public void testGetItemInfersMissingPlayer() {
        DynamoDbHandler handler = enabledHandler(new SyncDynamoDbClient());
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE)
                .key(item("Brazil", "Pele"))
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList(TABLE), DynamoDbOperationNames.GET_ITEM,
                request, true, 1L));

        handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, handler.getExecutionDto().failedQueries.size());
        assertAttribute(handler.getExecutionDto().failedQueries.get(0).attributes,
                "playerName", DynamoDbScalarTypeDto.STRING, "Pele");
        assertTrue(handler.getExecutionDto().failedQueries.get(0).missingPrimaryKeyAttributes.isEmpty());
    }

    @Test
    public void testDeduplicatesMissingKeyReportsAndRetainsSchemaAcrossReset() {
        SyncDynamoDbClient client = new SyncDynamoDbClient();
        DynamoDbHandler handler = enabledHandler(client);
        QueryRequest request = QueryRequest.builder().tableName("MatchScores")
                .keyConditionExpression("country = :country")
                .expressionAttributeValues(Collections.singletonMap(":country", AttributeValue.builder().s("Brazil").build()))
                .build();
        DynamoDbCommand command = new DynamoDbCommand(Collections.singletonList("MatchScores"),
                DynamoDbOperationNames.QUERY, request, true, 1L);
        handler.handle(command);
        handler.handle(command);
        handler.getEvaluatedDynamoDbCommands();
        assertEquals(1, handler.getExecutionDto().failedQueries.size());
        assertEquals(Collections.singletonMap("matchNumber", DynamoDbScalarTypeDto.NUMBER),
                handler.getExecutionDto().failedQueries.get(0).missingPrimaryKeyAttributes);

        handler.reset();
        handler.handle(command);
        handler.getEvaluatedDynamoDbCommands();
        assertEquals(1, handler.getExecutionDto().failedQueries.size());
        assertEquals(1, client.describeCalls);
    }

    @Test
    public void testReportsMissingPrimaryKeyForSecondaryIndexQuery() {
        DynamoDbHandler handler = enabledHandler(new SyncDynamoDbClient());
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":attempt", AttributeValue.builder().s("World Cup Final").build());
        values.put(":retry", AttributeValue.builder().s("1").build());
        QueryRequest request = QueryRequest.builder()
                .tableName("PaperTrackings")
                .indexName("attemptId-pcRetry-index")
                .keyConditionExpression("attemptId = :attempt AND pcRetry = :retry")
                .expressionAttributeValues(values)
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList("PaperTrackings"), DynamoDbOperationNames.QUERY,
                request, true, 1L));

        handler.getEvaluatedDynamoDbCommands();
        List<DynamoDbAttributeValueDto> attributes = handler.getExecutionDto().failedQueries.get(0).attributes;

        assertAttribute(attributes, "attemptId", DynamoDbScalarTypeDto.STRING, "World Cup Final");
        assertAttribute(attributes, "pcRetry", DynamoDbScalarTypeDto.STRING, "1");
        assertFalse(attributes.stream().anyMatch(attribute -> "trackingId".equals(attribute.attributeName)));
        assertEquals(Collections.singletonMap("trackingId", DynamoDbScalarTypeDto.STRING),
                handler.getExecutionDto().failedQueries.get(0).missingPrimaryKeyAttributes);
    }

    @Test
    public void testReportsMissingNumericPrimaryKey() {
        DynamoDbHandler handler = enabledHandler(new SyncDynamoDbClient());
        QueryRequest request = QueryRequest.builder()
                .tableName("MatchScores")
                .keyConditionExpression("country = :country")
                .expressionAttributeValues(Collections.singletonMap(":country", AttributeValue.builder().s("Brazil").build()))
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList("MatchScores"), DynamoDbOperationNames.QUERY,
                request, true, 1L));

        handler.getEvaluatedDynamoDbCommands();
        List<DynamoDbAttributeValueDto> attributes = handler.getExecutionDto().failedQueries.get(0).attributes;
        assertFalse(attributes.stream().anyMatch(attribute -> "matchNumber".equals(attribute.attributeName)));
        assertEquals(Collections.singletonMap("matchNumber", DynamoDbScalarTypeDto.NUMBER),
                handler.getExecutionDto().failedQueries.get(0).missingPrimaryKeyAttributes);
    }

    @Test
    public void testRejectsPlayerPredicatesThatCannotDefineAnInsertion() {
        for (String filter : new String[]{"fifaId > :number", "playerName = :player OR fifaId = :number",
                "country = :player", "profile.playerName = :player"}) {
            SyncDynamoDbClient client = new SyncDynamoDbClient();
            DynamoDbHandler handler = enabledHandler(client);
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":country", AttributeValue.builder().s("Argentina").build());
            values.put(":player", AttributeValue.builder().s("Lionel Scaloni").build());
            values.put(":number", AttributeValue.builder().n("10").build());
            QueryRequest request = QueryRequest.builder().tableName(TABLE)
                    .keyConditionExpression("country = :country").filterExpression(filter)
                    .expressionAttributeValues(values).build();
            handler.handle(new DynamoDbCommand(Collections.singletonList(TABLE), DynamoDbOperationNames.QUERY,
                    request, true, 1L));

            List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

            assertEquals(1, evaluated.size(), filter);
            assertFalse(evaluated.get(0).getDistanceWithMetrics().isEvaluationFailure(), filter);
            assertTrue(handler.getExecutionDto().failedQueries.isEmpty(), filter);
            assertEquals(0, client.describeCalls, filter);
        }
    }

    @Test
    public void testRejectsPlayerPrimaryKeyWithWrongType() {
        SyncDynamoDbClient client = new SyncDynamoDbClient();
        DynamoDbHandler handler = enabledHandler(client);
        QueryRequest request = QueryRequest.builder().tableName(TABLE)
                .keyConditionExpression("country = :country")
                .expressionAttributeValues(Collections.singletonMap(":country", AttributeValue.builder().n("10").build()))
                .build();
        handler.handle(new DynamoDbCommand(Collections.singletonList(TABLE), DynamoDbOperationNames.QUERY,
                request, true, 1L));

        List<DynamoDbCommandWithDistance> evaluated = handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, evaluated.size());
        assertFalse(evaluated.get(0).getDistanceWithMetrics().isEvaluationFailure());
        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
        assertEquals(1, client.describeCalls);
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

    private DynamoDbCommand nestedQueryCommand() {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":country", AttributeValue.builder().s("Argentina").build());
        values.put(":player", AttributeValue.builder().s("Lionel Scaloni").build());
        values.put(":fifaId", AttributeValue.builder().n("10").build());
        values.put(":captain", AttributeValue.builder().bool(true).build());
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression("country = :country")
                .filterExpression("playerName = :player AND (fifaId = :fifaId AND captain = :captain)")
                .expressionAttributeValues(values)
                .build();
        return new DynamoDbCommand(Collections.singletonList(TABLE), DynamoDbOperationNames.QUERY,
                request, true, 1L);
    }

    private void assertAttribute(
            List<DynamoDbAttributeValueDto> attributes,
            String name,
            DynamoDbScalarTypeDto type,
            String value) {
        DynamoDbAttributeValueDto attribute = attributes.stream()
                .filter(candidate -> name.equals(candidate.attributeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing attribute " + name));
        assertEquals(type, attribute.type);
        assertEquals(value, attribute.value);
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
        private int describeCalls;

        /** Returns the primary key schema for the World Cup and example application tables. */
        public DescribeTableResponse describeTable(DescribeTableRequest request) {
            describeCalls++;
            String hashKey = "country";
            String rangeKey = "playerName";
            ScalarAttributeType rangeType = ScalarAttributeType.S;
            if ("records".equals(request.tableName())) {
                hashKey = "subject_id";
                rangeKey = "record_key";
            } else if ("PaperTrackings".equals(request.tableName())) {
                hashKey = "trackingId";
                rangeKey = null;
            } else if ("MatchScores".equals(request.tableName())) {
                rangeKey = "matchNumber";
                rangeType = ScalarAttributeType.N;
            }
            TableDescription.Builder table = TableDescription.builder()
                    .keySchema(KeySchemaElement.builder().attributeName(hashKey).keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName(hashKey).attributeType(ScalarAttributeType.S).build());
            if (rangeKey != null) {
                table.keySchema(KeySchemaElement.builder().attributeName(hashKey).keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(rangeKey).keyType(KeyType.RANGE).build());
                table.attributeDefinitions(
                        AttributeDefinition.builder().attributeName(hashKey).attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(rangeKey).attributeType(rangeType).build());
            }
            return DescribeTableResponse.builder().table(table.build()).build();
        }

        /**
         * Returns two deterministic pages of World Cup player data.
         *
         * @param request scan request
         * @return scan response page
         */
        public ScanResponse scan(ScanRequest request) {
            scanCalls++;
            if ("records".equals(request.tableName())) {
                Map<String, AttributeValue> record = new HashMap<>();
                record.put("subject_id", AttributeValue.builder().s("Argentina").build());
                record.put("record_key", AttributeValue.builder().s("final-ticket").build());
                record.put("purpose", AttributeValue.builder().s("World Cup tickets").build());
                record.put("version", AttributeValue.builder().n("1").build());
                record.put("tombstoned", AttributeValue.builder().bool(false).build());
                Map<String, AttributeValue> sparseRecord = new HashMap<>();
                sparseRecord.put("subject_id", AttributeValue.builder().s("Uruguay").build());
                sparseRecord.put("record_key", AttributeValue.builder().s("semi-final-ticket").build());
                return ScanResponse.builder().items(sparseRecord, record).build();
            }
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
                    .lastEvaluatedKey(Collections.emptyMap())
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
