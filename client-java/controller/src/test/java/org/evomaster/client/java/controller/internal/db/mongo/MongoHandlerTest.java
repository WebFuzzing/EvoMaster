package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.Document;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.instrumentation.mongo.MongoLogger;
import org.junit.jupiter.api.*;

import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoHandlerTest extends MongoTestTemplate {

    private static InstrumentedSutStarter starter;
    private static MongoFakeSutController sutController;
    private static String url;

    @BeforeAll
    public static void startController() {
        sutController = buildSutController();
        starter = new InstrumentedSutStarter(sutController);
        url = start(starter);
        url += BASE_PATH;
    }

    @BeforeEach
    public void dropMongoDatabase() {
        mongoClient.getDatabase("testdb").drop();
        sutController.initMongoClient();
    }

    @AfterEach
    public void resetMongoHandler() {
        sutController.getMongoHandler().reset();
    }

    @AfterAll
    public static void stopController() {
        if (starter != null) {
            starter.stop();
        }
    }


    @Test
    public void testEmptyDistances() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.isEmpty());
    }

    @Test
    public void testFindNoResult() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase("testdb").getCollection("customers");

        // find({}) returns no element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument());

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.size());
    }


    @Test
    public void testTwoFindOperations() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase("testdb").getCollection("customers");

        // log find({}) operation
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument());

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.size());

        // log find({}) operation
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument());

        assertEquals(2, sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.size());
    }


    @Test
    public void testFindSomeResult() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase("testdb").getCollection("customers");

        // find({}) returns no element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument());

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.size());

        mongoCollection.insertOne(new Document());

        // log find({}) that returns an element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument());
        assertEquals(2, sutController.getMongoHandler().getMongoExecutionDto().mongoOperations.size());
    }

}
