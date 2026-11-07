package com.foo.somedifferentpackage.examples.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs a versioned Enhanced Client update through an instrumented AWS SDK.
 */
public class EnhancedFunctionApplyExample {

    private static final String UPDATE_ITEM = "updateItem";
    private static final String GET_ITEM = "getItem";
    private static final String PUT_ITEM = "putItem";
    private static final String DELETE_ITEM = "deleteItem";
    private static final String QUERY = "query";
    private static final String SCAN = "scan";
    private static final String BATCH_GET = "batch-get";
    private static final long PRODUCT_VERSION = 3L;

    /**
     * Executes a synchronous or asynchronous update against a local client proxy.
     * Invoked reflectively after an instrumenting class loader loads this class.
     *
     * @param async whether to use the asynchronous Enhanced Client
     * @return the exact low-level request passed to the client
     */
    public static Object updateVersionedProduct(boolean async) {
        AtomicReference<Object> request = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        Product product = new Product();
        product.setId("football");
        product.setPrice(100);
        product.setVersion(PRODUCT_VERSION);
        TableSchema<Product> schema = TableSchema.fromBean(Product.class);

        if (async) {
            DynamoDbAsyncClient client = (DynamoDbAsyncClient) Proxy.newProxyInstance(
                    DynamoDbAsyncClient.class.getClassLoader(),
                    new Class<?>[]{DynamoDbAsyncClient.class},
                    (proxy, method, args) -> {
                        if (UPDATE_ITEM.equals(method.getName())) {
                            calls.incrementAndGet();
                            request.set(args[0]);
                            return CompletableFuture.completedFuture(UpdateItemResponse.builder().build());
                        }
                        throw new AssertionError("Unexpected async client method: " + method.getName());
                    });
            DynamoDbEnhancedAsyncClient.builder()
                    .dynamoDbClient(client)
                    .extensions(VersionedRecordExtension.builder().build())
                    .build()
                    .table("Products", schema)
                    .updateItem(product)
                    .join();
        } else {
            DynamoDbClient client = (DynamoDbClient) Proxy.newProxyInstance(
                    DynamoDbClient.class.getClassLoader(),
                    new Class<?>[]{DynamoDbClient.class},
                    (proxy, method, args) -> {
                        if (UPDATE_ITEM.equals(method.getName())) {
                            calls.incrementAndGet();
                            request.set(args[0]);
                            return UpdateItemResponse.builder().build();
                        }
                        throw new AssertionError("Unexpected sync client method: " + method.getName());
                    });
            DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(client)
                    .extensions(VersionedRecordExtension.builder().build())
                    .build()
                    .table("Products", schema)
                    .updateItem(product);
        }

        if (calls.get() != 1) {
            throw new AssertionError("Expected one low-level update, observed " + calls.get());
        }
        return request.get();
    }

    /**
     * Executes an eager enhanced operation against a client proxy.
     * Invoked reflectively after an instrumenting class loader loads this class.
     *
     * @param operation getItem, putItem, or deleteItem
     * @param async whether to use the asynchronous Enhanced Client
     * @return the exact low-level request passed to the client
     */
    public static Object executePlayerOperation(String operation, boolean async) {
        AtomicReference<Object> request = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        Player player = new Player();
        player.setCountry("Argentina");
        player.setName("Lionel Messi");
        Key key = Key.builder().partitionValue("Argentina").sortValue("Lionel Messi").build();

        if (async) {
            DynamoDbAsyncClient client = (DynamoDbAsyncClient) Proxy.newProxyInstance(
                    DynamoDbAsyncClient.class.getClassLoader(),
                    new Class<?>[]{DynamoDbAsyncClient.class},
                    (proxy, method, args) -> {
                        if (!operation.equals(method.getName())) {
                            throw new AssertionError("Unexpected async client method: " + method.getName());
                        }
                        calls.incrementAndGet();
                        request.set(args[0]);
                        return CompletableFuture.completedFuture(playerResponse(operation));
                    });
            DynamoDbAsyncTable<Player> table = DynamoDbEnhancedAsyncClient.builder()
                    .dynamoDbClient(client).build().table("WorldCupPlayers", TableSchema.fromBean(Player.class));
            switch (operation) {
                case GET_ITEM: table.getItem(key).join(); break;
                case PUT_ITEM: table.putItem(player).join(); break;
                case DELETE_ITEM: table.deleteItem(key).join(); break;
                default: throw new IllegalArgumentException(operation);
            }
        } else {
            DynamoDbClient client = (DynamoDbClient) Proxy.newProxyInstance(
                    DynamoDbClient.class.getClassLoader(),
                    new Class<?>[]{DynamoDbClient.class},
                    (proxy, method, args) -> {
                        if (!operation.equals(method.getName())) {
                            throw new AssertionError("Unexpected sync client method: " + method.getName());
                        }
                        calls.incrementAndGet();
                        request.set(args[0]);
                        return playerResponse(operation);
                    });
            DynamoDbTable<Player> table = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(client).build().table("WorldCupPlayers", TableSchema.fromBean(Player.class));
            switch (operation) {
                case GET_ITEM: table.getItem(key); break;
                case PUT_ITEM: table.putItem(player); break;
                case DELETE_ITEM: table.deleteItem(key); break;
                default: throw new IllegalArgumentException(operation);
            }
        }
        if (calls.get() != 1) {
            throw new AssertionError("Expected one low-level " + operation + ", observed " + calls.get());
        }
        return request.get();
    }

    /** @return an empty low-level response for the requested operation */
    private static Object playerResponse(String operation) {
        switch (operation) {
            case GET_ITEM: return GetItemResponse.builder().build();
            case PUT_ITEM: return PutItemResponse.builder().build();
            case DELETE_ITEM: return DeleteItemResponse.builder().build();
            default: throw new IllegalArgumentException(operation);
        }
    }

    /**
     * Consumes all pages of an enhanced query, scan, or batch-get operation.
     * Invoked reflectively after an instrumenting class loader loads this class.
     *
     * @param endpoint DynamoDB Local endpoint
     * @param tableName table containing the players
     * @param operation query, scan, or batch-get
     * @param async whether to use the asynchronous Enhanced Client
     * @return number of pages consumed
     */
    public static int readPlayerPages(String endpoint, String tableName, String operation, boolean async) {
        URI uri = URI.create(endpoint);
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("dummy", "dummy"));
        if (async) {
            try (DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                    .endpointOverride(uri).region(Region.US_EAST_1).credentialsProvider(credentials).build()) {
                DynamoDbEnhancedAsyncClient enhanced = DynamoDbEnhancedAsyncClient.builder()
                        .dynamoDbClient(client).build();
                DynamoDbAsyncTable<Player> table = enhanced.table(tableName, TableSchema.fromBean(Player.class));
                AtomicInteger pages = new AtomicInteger();
                switch (operation) {
                    case QUERY:
                        enhancedPageCount(table.query(request -> request
                                .queryConditional(QueryConditional.keyEqualTo(
                                        Key.builder().partitionValue("Argentina").build()))
                                .limit(1)), pages);
                        break;
                    case SCAN:
                        enhancedPageCount(table.scan(request -> request.limit(1)), pages);
                        break;
                    case BATCH_GET:
                        enhancedPageCount(enhanced.batchGetItem(request -> request.addReadBatch(
                                playerReadBatch(table))), pages);
                        break;
                    default: throw new IllegalArgumentException(operation);
                }
                return pages.get();
            }
        }

        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(uri).region(Region.US_EAST_1).credentialsProvider(credentials).build()) {
            DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
            DynamoDbTable<Player> table = enhanced.table(tableName, TableSchema.fromBean(Player.class));
            switch (operation) {
                case QUERY:
                    return (int) table.query(request -> request
                            .queryConditional(QueryConditional.keyEqualTo(
                                    Key.builder().partitionValue("Argentina").build()))
                            .limit(1)).stream().count();
                case SCAN:
                    return (int) table.scan(request -> request.limit(1)).stream().count();
                case BATCH_GET:
                    return (int) enhanced.batchGetItem(request -> request.addReadBatch(
                            playerReadBatch(table))).stream().count();
                default: throw new IllegalArgumentException(operation);
            }
        }
    }

    /** @return a batch reading both Argentina players */
    private static ReadBatch playerReadBatch(MappedTableResource<Player> table) {
        return ReadBatch.builder(Player.class).mappedTableResource(table)
                .addGetItem(Key.builder().partitionValue("Argentina").sortValue("Lionel Messi").build())
                .addGetItem(Key.builder().partitionValue("Argentina").sortValue("Angel Di Maria").build())
                .build();
    }

    /** Counts published pages after the asynchronous operation completes. */
    private static void enhancedPageCount(software.amazon.awssdk.core.async.SdkPublisher<?> publisher,
                                          AtomicInteger pages) {
        publisher.subscribe(ignored -> pages.incrementAndGet()).join();
    }

    /**
     * Mapped product with optimistic locking.
     */
    @DynamoDbBean
    public static class Product {
        private String id;
        private int price;
        private Long version;

        /** @return product id */
        @DynamoDbPartitionKey
        public String getId() { return id; }

        /** @param id product id */
        public void setId(String id) { this.id = id; }

        /** @return product price */
        public int getPrice() { return price; }

        /** @param price product price */
        public void setPrice(int price) { this.price = price; }

        /** @return optimistic locking version */
        @DynamoDbVersionAttribute
        public Long getVersion() { return version; }

        /** @param version optimistic locking version */
        public void setVersion(Long version) { this.version = version; }
    }

    /**
     * Player mapping for a query that spans more than one page.
     */
    @DynamoDbBean
    public static class Player {
        private String country;
        private String name;

        /** @return country */
        @DynamoDbPartitionKey
        public String getCountry() { return country; }

        /** @param country country */
        public void setCountry(String country) { this.country = country; }

        /** @return player name */
        @DynamoDbSortKey
        public String getName() { return name; }

        /** @param name player name */
        public void setName(String name) { this.name = name; }
    }
}
