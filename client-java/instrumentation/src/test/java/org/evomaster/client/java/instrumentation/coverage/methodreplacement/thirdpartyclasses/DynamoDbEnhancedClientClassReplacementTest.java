package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests delegation and low-level request capture for enhanced DynamoDB replacements.
 */
public class DynamoDbEnhancedClientClassReplacementTest {

    private static final String PLAYERS_TABLE = "world_cup_players";
    private static final TableSchema<Player> PLAYER_SCHEMA = TableSchema.fromBean(Player.class);

    /**
     * Resets captured execution information before each test.
     */
    @BeforeEach
    public void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    /**
     * Verifies a synchronous get delegates a canonical request and records its low-level key.
     */
    @Test
    public void shouldRecordSynchronousGetByKey() {
        Key key = Key.builder().partitionValue("Argentina").sortValue("Lionel Messi").build();
        Player player = new Player("Argentina", "Lionel Messi");
        DynamoDbTable<Player> table = syncTable((proxy, method, arguments) -> {
            assertEquals("getItem", method.getName());
            assertTrue(arguments[0] instanceof GetItemEnhancedRequest);
            assertSame(key, ((GetItemEnhancedRequest) arguments[0]).key());
            return player;
        });

        Object result = DynamoDbEnhancedClientClassReplacement.SyncTable.getItem(table, key);

        assertSame(player, result);
        GetItemRequest request = recordedRequest(DynamoDbOperationNames.GET_ITEM, true, GetItemRequest.class);
        assertEquals("Argentina", request.key().get("country").s());
        assertEquals("Lionel Messi", request.key().get("playerName").s());
    }

    /**
     * Verifies an asynchronous put preserves the original future and records the mapped item.
     */
    @Test
    public void shouldRecordAsynchronousPut() {
        Player player = new Player("France", "Kylian Mbappe");
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        DynamoDbAsyncTable<Player> table = asyncTable((proxy, method, arguments) -> {
            assertEquals("putItem", method.getName());
            assertTrue(arguments[0] instanceof PutItemEnhancedRequest);
            assertSame(player, ((PutItemEnhancedRequest<?>) arguments[0]).item());
            return future;
        });

        Object result = DynamoDbEnhancedClientClassReplacement.AsyncTable.putItem(table, player);

        assertSame(future, result);
        PutItemRequest request = recordedRequest(DynamoDbOperationNames.PUT_ITEM, true, PutItemRequest.class);
        assertEquals("France", request.item().get("country").s());
        assertEquals("Kylian Mbappe", request.item().get("playerName").s());
    }

    /**
     * Verifies an application exception is propagated and its generated low-level request is recorded.
     */
    @Test
    public void shouldPropagateOriginalException() {
        IllegalStateException failure = new IllegalStateException("Argentina squad is unavailable");
        Key key = Key.builder().partitionValue("Argentina").sortValue("Angel Di Maria").build();
        DynamoDbTable<Player> table = syncTable((proxy, method, arguments) -> {
            throw failure;
        });

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> DynamoDbEnhancedClientClassReplacement.SyncTable.getItem(table, key));

        assertSame(failure, actual);
        GetItemRequest request = recordedRequest(DynamoDbOperationNames.GET_ITEM, false, GetItemRequest.class);
        assertEquals("Angel Di Maria", request.key().get("playerName").s());
    }

    /**
     * Verifies an exceptional asynchronous completion is recorded as unsuccessful.
     */
    @Test
    public void shouldRecordAsynchronousFailure() {
        Player player = new Player("England", "Jude Bellingham");
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("England squad is unavailable"));
        DynamoDbAsyncTable<Player> table = asyncTable((proxy, method, arguments) -> future);

        Object result = DynamoDbEnhancedClientClassReplacement.AsyncTable.putItem(table, player);

        assertSame(future, result);
        PutItemRequest request = recordedRequest(DynamoDbOperationNames.PUT_ITEM, false, PutItemRequest.class);
        assertEquals("Jude Bellingham", request.item().get("playerName").s());
    }

    /**
     * Returns the single low-level request captured by the execution tracer.
     *
     * @param operationName expected operation
     * @param successful expected completion status
     * @param requestType expected low-level request type
     * @param <T> low-level request type
     * @return captured low-level request
     */
    private <T> T recordedRequest(DynamoDbOperationNames operationName, boolean successful, Class<T> requestType) {
        List<AdditionalInfo> additionalInfo = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfo.size());
        Set<DynamoDbCommand> commands = additionalInfo.get(0).getDynamoDbInfoData();
        assertEquals(1, commands.size());

        DynamoDbCommand command = commands.iterator().next();
        assertEquals(Collections.singletonList(PLAYERS_TABLE), command.getTableNames());
        assertEquals(operationName, command.getOperationName());
        assertEquals(successful, command.isSuccessfullyExecuted());
        assertTrue(requestType.isInstance(command.getDdbRequest()));
        return requestType.cast(command.getDdbRequest());
    }

    /**
     * Creates a synchronous table proxy with the World Cup player schema.
     */
    private DynamoDbTable<Player> syncTable(InvocationHandler operationHandler) {
        return proxy(DynamoDbTable.class, tableHandler(operationHandler));
    }

    /**
     * Creates an asynchronous table proxy with the World Cup player schema.
     */
    private DynamoDbAsyncTable<Player> asyncTable(InvocationHandler operationHandler) {
        return proxy(DynamoDbAsyncTable.class, tableHandler(operationHandler));
    }

    /**
     * Adds mapped-table metadata behavior to an operation handler.
     */
    private InvocationHandler tableHandler(InvocationHandler operationHandler) {
        return (proxy, method, arguments) -> {
            if ("tableName".equals(method.getName())) {
                return PLAYERS_TABLE;
            }
            if ("tableSchema".equals(method.getName())) {
                return PLAYER_SCHEMA;
            }
            if ("mapperExtension".equals(method.getName())) {
                return null;
            }
            return operationHandler.invoke(proxy, method, arguments);
        };
    }

    /**
     * Creates a dynamic implementation of an enhanced-client interface.
     *
     * @param interfaceType enhanced-client interface
     * @param handler invocation behavior
     * @param <T> interface type
     * @return dynamic interface implementation
     */
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<?> interfaceType, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                handler);
    }

    /**
     * Mapped World Cup player used by the enhanced table schema.
     */
    @DynamoDbBean
    public static class Player {

        private String country;
        private String playerName;

        /**
         * Creates an empty bean for the enhanced mapper.
         */
        public Player() {
        }

        /**
         * Creates a mapped player.
         */
        public Player(String country, String playerName) {
            this.country = country;
            this.playerName = playerName;
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
