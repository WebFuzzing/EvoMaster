package com.foo.somedifferentpackage.examples.dynamodb;

import org.evomaster.client.java.instrumentation.example.dynamodb.EnhancedDynamoDbOperations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercises DynamoDB enhanced-client operations from code loaded through EvoMaster instrumentation.
 */
public class EnhancedDynamoDbOperationsImpl implements EnhancedDynamoDbOperations {

    private static final TableSchema<Player> PLAYER_SCHEMA = TableSchema.fromBean(Player.class);

    private final DynamoDbClient syncClient;
    private final DynamoDbAsyncClient asyncClient;
    private final DynamoDbEnhancedClient enhancedSyncClient;
    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient;
    private final DynamoDbTable<Player> syncTable;
    private final DynamoDbAsyncTable<Player> asyncTable;

    /**
     * Creates enhanced clients connected to DynamoDB Local.
     *
     * @param endpoint DynamoDB Local endpoint
     * @param tableName player table name
     */
    public EnhancedDynamoDbOperationsImpl(String endpoint, String tableName) {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("dummy", "dummy"));
        syncClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .build();
        asyncClient = DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .httpClientBuilder(NettyNioAsyncHttpClient.builder().protocol(Protocol.HTTP1_1))
                .build();
        enhancedSyncClient = DynamoDbEnhancedClient.builder().dynamoDbClient(syncClient).build();
        enhancedAsyncClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(asyncClient).build();
        syncTable = enhancedSyncClient.table(tableName, PLAYER_SCHEMA);
        asyncTable = enhancedAsyncClient.table(tableName, PLAYER_SCHEMA);
    }

    /**
     * Executes a named enhanced-client operation.
     *
     * @param operation operation identifier
     * @return actual number of consumed pages, or one for a non-paginated operation
     */
    @Override
    public int execute(String operation) {
        switch (operation) {
            case "sync-get":
                syncTable.getItem(key("Argentina", "Lionel Messi"));
                return 1;
            case "async-get":
                asyncTable.getItem(key("France", "Kylian Mbappe")).join();
                return 1;
            case "sync-batch-get":
                return syncBatchGet();
            case "async-batch-get":
                return asyncBatchGet();
            case "sync-put":
                syncTable.putItem(player("Brazil", "Neymar Junior", 32));
                return 1;
            case "async-put":
                asyncTable.putItem(player("England", "Jude Bellingham", 21)).join();
                return 1;
            case "sync-update":
                syncTable.updateItem(player("Argentina", "Lionel Messi", 37));
                return 1;
            case "async-update":
                asyncTable.updateItem(player("France", "Kylian Mbappe", 26)).join();
                return 1;
            case "sync-delete":
                syncTable.deleteItem(key("Portugal", "Cristiano Ronaldo"));
                return 1;
            case "async-delete":
                asyncTable.deleteItem(key("Spain", "Pedri Gonzalez")).join();
                return 1;
            case "sync-query":
                return syncQuery();
            case "async-query":
                return asyncQuery();
            case "sync-scan":
                return syncScan();
            case "async-scan":
                return asyncScan();
            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
    }

    /**
     * Executes a conditional put against an existing World Cup player.
     *
     * @param async whether to use the asynchronous enhanced client
     */
    @Override
    public void executeConditionalFailure(boolean async) {
        PutItemEnhancedRequest<Player> request = PutItemEnhancedRequest.builder(Player.class)
                .item(player("Argentina", "Lionel Messi", 37))
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#country)")
                        .expressionNames(Collections.singletonMap("#country", "country"))
                        .build())
                .build();
        if (async) {
            asyncTable.putItem(request).join();
        } else {
            syncTable.putItem(request);
        }
    }

    /**
     * Closes both low-level clients.
     */
    @Override
    public void close() {
        syncClient.close();
        asyncClient.close();
    }

    /**
     * Executes and consumes a synchronous enhanced batch-get paginator.
     *
     * @return consumed page count
     */
    private int syncBatchGet() {
        ReadBatch readBatch = ReadBatch.builder(Player.class)
                .mappedTableResource(syncTable)
                .addGetItem(key("Argentina", "Lionel Messi"))
                .addGetItem(key("Argentina", "Angel Di Maria"))
                .build();
        BatchGetResultPageIterable pages = enhancedSyncClient.batchGetItem(
                request -> request.addReadBatch(readBatch));
        return (int) pages.stream().count();
    }

    /**
     * Executes and consumes an asynchronous enhanced batch-get publisher.
     *
     * @return consumed page count
     */
    private int asyncBatchGet() {
        ReadBatch readBatch = ReadBatch.builder(Player.class)
                .mappedTableResource(asyncTable)
                .addGetItem(key("France", "Kylian Mbappe"))
                .addGetItem(key("France", "Antoine Griezmann"))
                .build();
        BatchGetResultPagePublisher pages = enhancedAsyncClient.batchGetItem(
                request -> request.addReadBatch(readBatch));
        return consume(pages);
    }

    /**
     * Executes and consumes a synchronous enhanced query with a one-item page limit.
     *
     * @return consumed page count
     */
    private int syncQuery() {
        PageIterable<Player> pages = syncTable.query(request -> request
                .queryConditional(QueryConditional.keyEqualTo(key -> key.partitionValue("Argentina")))
                .limit(1));
        return (int) pages.stream().count();
    }

    /**
     * Executes and consumes an asynchronous enhanced query with a one-item page limit.
     *
     * @return consumed page count
     */
    private int asyncQuery() {
        PagePublisher<Player> pages = asyncTable.query(request -> request
                .queryConditional(QueryConditional.keyEqualTo(key -> key.partitionValue("France")))
                .limit(1));
        return consume(pages);
    }

    /**
     * Executes and consumes a synchronous enhanced scan with a two-item page limit.
     *
     * @return consumed page count
     */
    private int syncScan() {
        return (int) syncTable.scan(request -> request.limit(2)).stream().count();
    }

    /**
     * Executes and consumes an asynchronous enhanced scan with a two-item page limit.
     *
     * @return consumed page count
     */
    private int asyncScan() {
        return consume(asyncTable.scan(request -> request.limit(2)));
    }

    /**
     * Consumes every element from an SDK publisher.
     *
     * @param publisher publisher to consume
     * @return consumed element count
     */
    private int consume(software.amazon.awssdk.core.async.SdkPublisher<?> publisher) {
        AtomicInteger count = new AtomicInteger();
        publisher.subscribe(ignored -> count.incrementAndGet()).join();
        return count.get();
    }

    /**
     * Creates a composite player key.
     *
     * @param country World Cup country
     * @param name player name
     * @return enhanced DynamoDB key
     */
    private Key key(String country, String name) {
        return Key.builder().partitionValue(country).sortValue(name).build();
    }

    /**
     * Creates a player bean.
     *
     * @param country World Cup country
     * @param name player name
     * @param age player age
     * @return player bean
     */
    private Player player(String country, String name, int age) {
        Player player = new Player();
        player.setCountry(country);
        player.setName(name);
        player.setAge(age);
        return player;
    }

    /**
     * World Cup player mapped to the DynamoDB test table.
     */
    @DynamoDbBean
    public static class Player {

        private String country;
        private String name;
        private int age;

        /**
         * @return player's country
         */
        @DynamoDbPartitionKey
        public String getCountry() {
            return country;
        }

        /**
         * @param country player's country
         */
        public void setCountry(String country) {
            this.country = country;
        }

        /**
         * @return player's name
         */
        @DynamoDbSortKey
        public String getName() {
            return name;
        }

        /**
         * @param name player's name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return player's age
         */
        public int getAge() {
            return age;
        }

        /**
         * @param age player's age
         */
        public void setAge(int age) {
            this.age = age;
        }
    }
}
