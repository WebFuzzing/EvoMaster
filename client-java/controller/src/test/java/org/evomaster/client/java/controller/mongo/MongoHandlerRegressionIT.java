package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.evomaster.client.java.controller.internal.db.mongo.MongoCommandWithDistance;
import org.evomaster.client.java.controller.internal.db.mongo.MongoHandler;
import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Live regressions for Mongo execution tracking, isolated even when using an external server. */
class MongoHandlerRegressionIT {
    private static GenericContainer<?> container;
    private static MongoClient client;
    private static MongoDatabase database;
    private MongoCollection<Document> collection;

    @BeforeAll
    static void startMongo() {
        String uri = System.getProperty("evomaster.mongo.uri");
        if (uri == null || uri.isEmpty()) {
            container = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
            container.start();
            uri = "mongodb://" + container.getHost() + ":" + container.getMappedPort(27017);
        }
        client = MongoClients.create(uri);
        database = client.getDatabase("handler_regressions_" + UUID.randomUUID().toString().replace("-", ""));
        database.runCommand(new Document("ping", 1));
    }

    @BeforeEach
    void createCollection() {
        collection = database.getCollection("items_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @AfterEach
    void removeCollectionAndLeakedCursors() {
        if (collection == null) {
            return;
        }
        try {
            // Dropping the database alone does not close the abandoned server cursors.
            List<Long> cursorIds = idleCursors().stream()
                    .map(operation -> ((Number) operation.get("cursor", Document.class).get("cursorId")).longValue())
                    .collect(Collectors.toList());
            if (!cursorIds.isEmpty()) {
                database.runCommand(new Document("killCursors", collection.getNamespace().getCollectionName())
                        .append("cursors", cursorIds));
            }
        } finally {
            collection.drop();
        }
    }

    @AfterAll
    static void stopMongo() {
        try {
            if (database != null) {
                database.drop();
            }
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } finally {
                if (container != null) {
                    container.stop();
                }
            }
        }
    }

    @Test
    void shouldCloseEveryCursorUsedToEvaluateACollection() {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            documents.add(new Document("value", i));
        }
        collection.insertMany(documents);

        // This control crosses the default first batch and consumes the server cursor fully.
        assertEquals(250, collection.find().batchSize(101).into(new ArrayList<>()).size());
        assertTrue(idleCursors().isEmpty(), "The ordinary fully consumed find must close its cursor");

        MongoHandler handler = handlerFor(new Document());
        assertEquals(1, handler.getEvaluatedMongoCommands().size());

        assertTrue(idleCursors().isEmpty(), "Evaluating a collection must not leave an idle server cursor");
    }

    @Test
    void shouldTrackUnfilteredFindLikeAnExplicitEmptyFilter() {
        assertNull(collection.find().first());
        assertNull(collection.find(new Document()).first());

        MongoHandler explicitFilterHandler = handlerFor(new Document());
        int expectedEvaluatedCommands = explicitFilterHandler.getEvaluatedMongoCommands().size();
        int expectedFailedQueries = explicitFilterHandler.getExecutionDto().failedQueries.size();
        assertEquals(1, expectedEvaluatedCommands);
        assertEquals(1, expectedFailedQueries);

        // MongoCollectionClassReplacement records the no-argument find with a null query.
        MongoHandler unfilteredHandler = handlerFor(null);
        int actualEvaluatedCommands = unfilteredHandler.getEvaluatedMongoCommands().size();
        int actualFailedQueries = unfilteredHandler.getExecutionDto().failedQueries.size();
        assertAll(
                () -> assertEquals(expectedEvaluatedCommands, actualEvaluatedCommands,
                        "Unfiltered find must be evaluated like find({})"),
                () -> assertEquals(expectedFailedQueries, actualFailedQueries,
                        "Unfiltered find must report the empty collection for data generation")
        );
    }

    private MongoHandler handlerFor(Object query) {
        MongoHandler handler = new MongoHandler();
        handler.setMongoClient(client);
        handler.handle(new MongoFindCommand(database.getName(), collection.getNamespace().getCollectionName(),
                "{}", query, true, 0));
        return handler;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedDriverFilters")
    void shouldEvaluateSupportedJavaDriverFilters(String name, Bson filter) {
        collection.insertOne(new Document("value", 1));
        assertEquals(1, collection.find(filter).into(new ArrayList<>()).size(),
                "The actual Java-driver query must match before testing its recorded command");

        List<MongoCommandWithDistance> control = handlerFor(new Document("value", 1)).getEvaluatedMongoCommands();
        assertEquals(1, control.size());
        assertEquals(0.0, control.get(0).mongoDistanceWithMetrics.mongoDistance);

        // Instrumentation records the original Bson filter, without converting it to Document.
        List<MongoCommandWithDistance> evaluated = assertDoesNotThrow(() -> handlerFor(filter).getEvaluatedMongoCommands());
        assertEquals(1, evaluated.size());
        assertEquals(0.0, evaluated.get(0).mongoDistanceWithMetrics.mongoDistance);
    }

    private static Stream<Arguments> supportedDriverFilters() {
        return Stream.of(
                Arguments.of("Filters.eq builder", Filters.eq("value", 1)),
                Arguments.of("BsonDocument filter", new BsonDocument("value", new BsonInt32(1)))
        );
    }

    private List<Document> idleCursors() {
        return client.getDatabase("admin").aggregate(Arrays.asList(
                new Document("$currentOp", new Document("allUsers", true).append("idleCursors", true)),
                new Document("$match", new Document("type", "idleCursor")
                        .append("ns", collection.getNamespace().getFullName()))
        )).into(new ArrayList<>());
    }
}
