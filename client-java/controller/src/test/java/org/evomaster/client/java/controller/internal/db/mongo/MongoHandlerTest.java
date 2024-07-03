package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.evomaster.client.java.controller.internal.db.MongoHandler;
import org.evomaster.client.java.controller.internal.db.MongoOperationDistance;
import org.evomaster.client.java.instrumentation.MongoInfo;
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
    public static void initClass() throws Exception {
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
    public void testGetDistances() {
        Document document = new Document("name", "John Doe")
                .append("age", 30)
                .append("city", "New York");

        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        assertFalse(collection.find().iterator().hasNext());

        collection.insertOne(document);
        assertTrue(collection.find().iterator().hasNext());

        List<Document> documents = new ArrayList<>();
        collection.find().into(documents);
        assertEquals(1, documents.size());

        final Bson bsonQuery = eq("age", 18);
        Document queryDocument = MongoHeuristicCalculatorTest.convertToDocument( bsonQuery);

        assertFalse(collection.find(queryDocument ).iterator().hasNext());

        final String documentsType = null;
        final boolean successfullyExecuted = true;
        final int executionTime = 1;

        MongoInfo mongoInfo = new MongoInfo(DATABASE_NAME,
                COLLECTION_NAME,
                documentsType,
                queryDocument,
                successfullyExecuted,
                executionTime);

        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setMongoClient(mongoClient);
        mongoHandler.setCalculateHeuristics(true);
        mongoHandler.setExtractMongoExecution(true);
        mongoHandler.handle(mongoInfo);
        List<MongoOperationDistance> distances = mongoHandler.getDistances();

        assertEquals(1, distances.size());

        MongoOperationDistance distance = distances.iterator().next();
        assertEquals(queryDocument, distance.bson);
        assertEquals(Math.abs(30 - 18), distance.distance);
    }
}
