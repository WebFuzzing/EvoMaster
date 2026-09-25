package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculatorTest;
import org.evomaster.client.java.controller.mongo.MongoQueryTestCases;
import org.evomaster.client.java.instrumentation.MongoCollectionSchema;
import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.size;

/** Mongo execution tracking and live query expectations, isolated on an external server or container. */
public class MongoHandlerTest {
    private static GenericContainer<?> container;
    private static MongoClient client;
    private static MongoDatabase database;
    private MongoCollection<Document> collection;
    private boolean inspectCursorLeaks;

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
        collection.drop();
    }

    @AfterEach
    void removeCollectionAndLeakedCursors() {
        if (collection == null) {
            return;
        }
        try {
            // Dropping the database alone does not close the abandoned server cursors.
            if (inspectCursorLeaks) {
                List<Long> cursorIds = idleCursors().stream()
                        .map(operation -> ((Number) operation.get("cursor", Document.class).get("cursorId")).longValue())
                        .collect(Collectors.toList());
                if (!cursorIds.isEmpty()) {
                    database.runCommand(new Document("killCursors", collection.getNamespace().getCollectionName())
                            .append("cursors", cursorIds));
                }
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
    public void testGetEvaluatedMongoCommands() {
        Document document = new Document("name", "John Doe")
                .append("age", 30)
                .append("city", "New York");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            assertFalse(cursor.hasNext());
        }

        collection.insertOne(document);

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            assertTrue(cursor.hasNext());
        }

        List<Document> documents = new ArrayList<>();
        collection.find().into(documents);
        assertEquals(1, documents.size());

        final Bson bsonQuery = eq("age", 18);
        Document queryDocument = MongoHeuristicsCalculatorTest.convertToDocument(bsonQuery);

        try (MongoCursor<Document> cursor = collection.find(queryDocument).iterator()) {
            assertFalse(cursor.hasNext());
        }

        final boolean successfullyExecuted = true;
        final int executionTime = 1;

        MongoFindCommand mongoFindCommand = new MongoFindCommand(database.getName(),
                collection.getNamespace().getCollectionName(),
                null,
                queryDocument,
                successfullyExecuted,
                executionTime);

        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setMongoClient(client);
        mongoHandler.setCalculateHeuristics(true);
        mongoHandler.setExtractMongoExecution(true);
        mongoHandler.handle(mongoFindCommand);
        List<MongoCommandWithDistance> mongoCommandWithDistances = mongoHandler.getEvaluatedMongoCommands();

        assertEquals(1, mongoCommandWithDistances.size());

        MongoCommandWithDistance mongoCommandWithDistance = mongoCommandWithDistances.iterator().next();
        assertEquals(queryDocument, mongoCommandWithDistance.mongoCommand);
        // Distances have changed due to new truthness-based heuristics
        assertTrue(mongoCommandWithDistance.mongoDistanceWithMetrics.mongoDistance > 0.0);
        assertEquals(1, mongoCommandWithDistance.mongoDistanceWithMetrics.numberOfEvaluatedDocuments);
    }

    @Test
    public void testEvaluateCommandsIgnoreInvalidQueries() {

        Document invalidQueryDocument = MongoHeuristicsCalculatorTest.convertToDocument(size("tags", -1));
        final boolean successfullyExecuted = false;
        final int executionTime = 1;

        MongoFindCommand mongoFindCommand = new MongoFindCommand(database.getName(),
                collection.getNamespace().getCollectionName(),
                null,
                invalidQueryDocument,
                successfullyExecuted,
                executionTime);

        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setMongoClient(client);
        mongoHandler.setCalculateHeuristics(true);
        mongoHandler.setExtractMongoExecution(true);
        mongoHandler.handle(mongoFindCommand);
        List<MongoCommandWithDistance> mongoCommandWithDistances = mongoHandler.getEvaluatedMongoCommands();

        assertTrue(mongoCommandWithDistances.isEmpty());
    }

    @Test
    void shouldCloseEveryCursorUsedToEvaluateACollection() {
        inspectCursorLeaks = true;
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

    @Test
    void shouldKeepSchemasSeparateForCollectionsInDifferentDatabases() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        MongoDatabase customers = client.getDatabase("handler_customers_" + suffix);
        MongoDatabase invoices = client.getDatabase("handler_invoices_" + suffix);
        String collectionName = "items_" + suffix;
        String customerSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(
                CustomerDocument.class, true, Collections.emptyList());
        String invoiceSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(
                InvoiceDocument.class, true, Collections.emptyList());

        try {
            Document customerQuery = new Document("customerEmail", "customer@example.com");
            Document invoiceQuery = new Document("invoiceTotal", 10);
            assertNull(customers.getCollection(collectionName).find(customerQuery).first());
            assertNull(invoices.getCollection(collectionName).find(invoiceQuery).first());
            assertNotEquals(customerSchema, invoiceSchema);

            MongoHandler handler = new MongoHandler();
            handler.setMongoClient(client);
            handler.handle(new MongoFindCommand(customers.getName(), collectionName,
                    customerSchema, customerQuery, true, 0));
            handler.handle(new MongoFindCommand(invoices.getName(), collectionName,
                    invoiceSchema, invoiceQuery, true, 0));
            assertEquals(2, handler.getEvaluatedMongoCommands().size());

            Map<String, String> fallbackSchemas = handler.getExecutionDto().failedQueries.stream()
                    .collect(Collectors.toMap(query -> query.getDatabase(), query -> query.getDocumentsType()));
            assertEquals(customerSchema, fallbackSchemas.get(customers.getName()));
            assertEquals(invoiceSchema, fallbackSchemas.get(invoices.getName()));

            // Framework registrations currently identify collections without their database.
            handler.handle(new MongoCollectionSchema(collectionName, customerSchema));
            handler.handle(new MongoCollectionSchema(collectionName, invoiceSchema));
            Map<String, String> registeredSchemas = handler.getExecutionDto().failedQueries.stream()
                    .collect(Collectors.toMap(query -> query.getDatabase(), query -> query.getDocumentsType()));
            assertAll(
                    () -> assertEquals(customerSchema, registeredSchemas.get(customers.getName()),
                            "The other database's collection registration must not replace the customer schema"),
                    () -> assertEquals(invoiceSchema, registeredSchemas.get(invoices.getName()))
            );
        } finally {
            try {
                customers.drop();
            } finally {
                invoices.drop();
            }
        }
    }

    public static class CustomerDocument {
        public String customerEmail;
    }

    public static class InvoiceDocument {
        public int invoiceTotal;
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

    @ParameterizedTest(name = "MongoDB: {0}")
    @MethodSource("org.evomaster.client.java.controller.mongo.MongoQueryTestCases#scenarios")
    void shouldConfirmExpectedMatchOnLiveMongo(MongoQueryTestCases.Case scenario) {
        if (scenario.index() != null) {
            collection.createIndex(scenario.index());
        }
        // The bundled 4.2 driver predates MongoDB 5's support for dollar-prefixed stored keys.
        // Let the live server validate the fixture through its ordinary insert command.
        Document inserted = database.runCommand(new Document("insert", collection.getNamespace().getCollectionName())
                .append("documents", Collections.singletonList(scenario.document())));
        assertFalse(inserted.containsKey("writeErrors"), inserted.toJson());
        assertEquals(1, inserted.getInteger("n").intValue());
        // Use find rather than countDocuments: the latter runs an aggregation that disallows $near.
        assertEquals(scenario.matches ? 1 : 0, collection.find(scenario.query()).into(new ArrayList<>()).size());
    }

    @Test
    void shouldConfirmDetailedLinesAreDisjoint() {
        collection.insertOne(new Document("loc", MongoQueryTestCases.detailedLine(2000, 0)));
        assertEquals(0L, collection.countDocuments(MongoQueryTestCases.geo("$geoIntersects",
                MongoQueryTestCases.detailedLine(2000, 1))));
        assertEquals(1L, collection.countDocuments(MongoQueryTestCases.geo("$geoIntersects",
                MongoQueryTestCases.detailedLine(2000, 0))));
    }
}
