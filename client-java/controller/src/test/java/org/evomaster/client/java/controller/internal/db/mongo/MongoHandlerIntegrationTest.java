package org.evomaster.client.java.controller.internal.db.mongo;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.bson.BsonDocument;
import org.bson.Document;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.WrappedResponseDto;
import org.evomaster.client.java.controller.api.dto.database.execution.FindOperationDto;
import org.evomaster.client.java.controller.api.dto.database.execution.FindResultDto;
import org.evomaster.client.java.instrumentation.mongo.MongoLogger;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.MONGO_COMMAND;
import static org.junit.jupiter.api.Assertions.*;

public class MongoHandlerIntegrationTest extends MongoTestTemplate {

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
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());
    }

    @Test
    public void testFindNoResult() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase("testdb").getCollection("customers");

        // find({}) returns no element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());
    }


    @Test
    public void testTwoFindOperations() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase("testdb").getCollection("customers");

        // log find({}) operation
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());

        // log find({}) operation
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(2, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());
    }


    @Test
    public void testFindSomeResult() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase("testdb").getCollection("customers");

        // find({}) returns no element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());

        mongoCollection.insertOne(new Document());

        // log find({}) that returns an element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);
        assertEquals(2, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());
    }

    @Test
    public void postMongoCommand() {
        startNewTest(url);

        FindOperationDto requestDto = new FindOperationDto();
        requestDto.databaseName = "mydb";
        requestDto.collectionName = "mycollection";
        requestDto.queryJsonStr = new BsonDocument().toJson();

        this.mongoClient.getDatabase("mydb").getCollection("mycollection").deleteMany(new BsonDocument());

        String jsonString = given().contentType(ContentType.JSON)
                .body(requestDto)
                .post(url + MONGO_COMMAND)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(WrappedResponseDto.class).data.toString();

        FindResultDto responseDto = new Gson().fromJson(jsonString, FindResultDto.class);

        assertFalse(responseDto.hasReturnedAnyDocument);
    }

    @Test
    public void postMongoCommandOnNonEmpty() {
        startNewTest(url);

        Document anEmptyDocument = new Document();
        this.mongoClient.getDatabase("mydb").getCollection("mycollection").insertOne(anEmptyDocument);

        FindOperationDto requestDto = new FindOperationDto();
        requestDto.databaseName = "mydb";
        requestDto.collectionName = "mycollection";
        requestDto.queryJsonStr = new BsonDocument().toJson();

        String jsonString = given().contentType(ContentType.JSON)
                .body(requestDto)
                .post(url + MONGO_COMMAND)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(WrappedResponseDto.class).data.toString();

        FindResultDto responseDto = new Gson().fromJson(jsonString, FindResultDto.class);

        assertTrue(responseDto.hasReturnedAnyDocument);
    }
}
