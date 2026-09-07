package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.DynamoDbEnhancedClientClassReplacement;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises enhanced-client interception through the complete DynamoDB heuristics pipeline.
 */
public class DynamoDbEnhancedClientHeuristicsTest {

    private static final String PLAYERS_TABLE = "WorldCupPlayers";
    private static final TableSchema<Player> PLAYER_SCHEMA = TableSchema.fromBean(Player.class);

    /**
     * Clears commands captured by earlier tests.
     */
    @BeforeEach
    public void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    /**
     * Verifies an enhanced query becomes a low-level query and produces a zero heuristic distance.
     */
    @Test
    public void shouldCalculateHeuristicsForEnhancedClientQuery() {
        AtomicInteger consumerCalls = new AtomicInteger();
        DynamoDbTable<Player> table = tableProxy();
        Consumer<QueryEnhancedRequest.Builder> query = request -> {
            consumerCalls.incrementAndGet();
            request.queryConditional(QueryConditional.keyEqualTo(
                    Key.builder().partitionValue("Argentina").build()));
            request.filterExpression(Expression.builder()
                    .expression("playerName = :player")
                    .putExpressionValue(":player", stringValue("Lionel Messi"))
                    .build());
        };

        DynamoDbEnhancedClientClassReplacement.SyncTable.query(table, query);

        DynamoDbCommand command = capturedCommand();
        assertEquals(1, consumerCalls.get());
        assertTrue(command.getDdbRequest() instanceof QueryRequest);
        QueryRequest lowLevelRequest = (QueryRequest) command.getDdbRequest();
        assertEquals(PLAYERS_TABLE, lowLevelRequest.tableName());
        assertTrue(lowLevelRequest.keyConditionExpression().contains("country"));
        assertEquals("playerName = :player", lowLevelRequest.filterExpression());

        DynamoDbHandler handler = new DynamoDbHandler();
        handler.setDynamoDbClient(new WorldCupDynamoDbClient());
        handler.handle(command);
        List<DynamoDbCommandWithDistance> results = handler.getEvaluatedDynamoDbCommands();

        assertEquals(1, results.size());
        DynamoDbDistanceWithMetrics metrics = results.get(0).getDistanceWithMetrics();
        assertEquals(0.0d, metrics.getDistance(), 0.0d);
        assertEquals(2, metrics.getNumberOfEvaluatedItems());
        assertFalse(metrics.isEvaluationFailure());
    }

    /**
     * Creates an enhanced table that exposes mapping metadata without contacting DynamoDB.
     */
    @SuppressWarnings("unchecked")
    private DynamoDbTable<Player> tableProxy() {
        return (DynamoDbTable<Player>) Proxy.newProxyInstance(
                DynamoDbTable.class.getClassLoader(),
                new Class<?>[]{DynamoDbTable.class},
                (proxy, method, arguments) -> {
                    if ("tableName".equals(method.getName())) {
                        return PLAYERS_TABLE;
                    }
                    if ("tableSchema".equals(method.getName())) {
                        return PLAYER_SCHEMA;
                    }
                    if ("mapperExtension".equals(method.getName())) {
                        return null;
                    }
                    if ("query".equals(method.getName())) {
                        assertTrue(arguments[0] instanceof QueryEnhancedRequest);
                        return null;
                    }
                    throw new AssertionError("Unexpected enhanced table call: " + method.getName());
                });
    }

    /**
     * Returns the single command captured from the replacement.
     */
    private DynamoDbCommand capturedCommand() {
        List<AdditionalInfo> additionalInfo = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfo.size());
        assertEquals(1, additionalInfo.get(0).getDynamoDbInfoData().size());
        return additionalInfo.get(0).getDynamoDbInfoData().iterator().next();
    }

    /**
     * Creates a DynamoDB string value.
     */
    private static AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    /**
     * Creates one low-level item returned by the deterministic table scan.
     */
    private static Map<String, AttributeValue> item(String country, String playerName) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("country", stringValue(country));
        item.put("playerName", stringValue(playerName));
        return item;
    }

    /**
     * Minimal low-level client supplying the table contents used by the calculator.
     */
    public static final class WorldCupDynamoDbClient {

        /**
         * Returns deterministic World Cup player rows.
         *
         * @param request table scan request
         * @return table contents
         */
        public ScanResponse scan(ScanRequest request) {
            assertEquals(PLAYERS_TABLE, request.tableName());
            return ScanResponse.builder()
                    .items(Arrays.asList(
                            item("Argentina", "Diego Maradona"),
                            item("Argentina", "Lionel Messi")))
                    .lastEvaluatedKey(Collections.<String, AttributeValue>emptyMap())
                    .build();
        }
    }

    /**
     * Mapped player used to build the enhanced query key expression.
     */
    @DynamoDbBean
    public static class Player {

        private String country;
        private String playerName;

        /**
         * Creates an empty mapper bean.
         */
        public Player() {
        }

        /**
         * @return national team country
         */
        @DynamoDbPartitionKey
        public String getCountry() {
            return country;
        }

        /**
         * @param country national team country
         */
        public void setCountry(String country) {
            this.country = country;
        }

        /**
         * @return player name
         */
        @DynamoDbSortKey
        public String getPlayerName() {
            return playerName;
        }

        /**
         * @param playerName player name
         */
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }
    }
}
