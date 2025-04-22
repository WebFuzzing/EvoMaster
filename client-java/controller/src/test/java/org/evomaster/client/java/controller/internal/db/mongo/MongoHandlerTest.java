package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.evomaster.client.java.controller.internal.db.MongoHandler;
import org.evomaster.client.java.controller.internal.db.MongoCommandWithDistance;
import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MongoHandlerTest {

    private static MongoClient mongoClient;
    private static final int MONGODB_PORT = 27017;
    private static final GenericContainer<?> mongodb = new GenericContainer<>("mongo:6.0")
            .withExposedPorts(MONGODB_PORT);

    @BeforeAll
    public static void initClass()  {
        mongodb.start();
        int port = mongodb.getMappedPort(MONGODB_PORT);

        mongoClient = MongoClients.create("mongodb://localhost:" + port + "/" + "aDatabase");
    }

    private final static String DATABASE_NAME = "myDatabase";
    private final static String COLLECTION_NAME = "myCollection";

    @BeforeEach
    public void clearDatabase() {

        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        // delete all documents in collection (if any)
        collection.deleteMany(new Document());

    }

    @Test
    public void testGetEvaluatedMongoCommands() {
        Document document = new Document("name", "John Doe")
                .append("age", 30)
                .append("city", "New York");

        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

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
        Document queryDocument = MongoHeuristicCalculatorTest.convertToDocument( bsonQuery);

        try (MongoCursor<Document> cursor = collection.find(queryDocument).iterator()) {
            assertFalse(cursor.hasNext());
        }

        final boolean successfullyExecuted = true;
        final int executionTime = 1;

        MongoFindCommand mongoFindCommand = new MongoFindCommand(DATABASE_NAME,
                COLLECTION_NAME,
                null,
                queryDocument,
                successfullyExecuted,
                executionTime);

        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setMongoClient(mongoClient);
        mongoHandler.setCalculateHeuristics(true);
        mongoHandler.setExtractMongoExecution(true);
        mongoHandler.handle(mongoFindCommand);
        List<MongoCommandWithDistance> mongoCommandWithDistances = mongoHandler.getEvaluatedMongoCommands();

        assertEquals(1, mongoCommandWithDistances.size());

        MongoCommandWithDistance mongoCommandWithDistance = mongoCommandWithDistances.iterator().next();
        assertEquals(queryDocument, mongoCommandWithDistance.mongoCommand);
        assertEquals(Math.abs(30 - 18), mongoCommandWithDistance.mongoDistanceWithMetrics.mongoDistance);
        assertEquals(1, mongoCommandWithDistance.mongoDistanceWithMetrics.numberOfEvaluatedDocuments);
    }
}
