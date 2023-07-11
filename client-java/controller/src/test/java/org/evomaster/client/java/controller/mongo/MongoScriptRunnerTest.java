package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class MongoScriptRunnerTest {

    private static MongoClient connection;
    private static final int MONGODB_PORT = 27017;
    private static final GenericContainer<?> mongodb = new GenericContainer<>("mongo:6.0")
            .withExposedPorts(MONGODB_PORT);

    @BeforeAll
    public static void initClass() throws Exception {
        mongodb.start();
        int port = mongodb.getMappedPort(MONGODB_PORT);

        connection = MongoClients.create("mongodb://localhost:" + port + "/" + "aDatabase");
    }

    @Test
    public void testInsert() {
        assertFalse(connection.getDatabase("aDatabase").getCollection("aCollection").find().cursor().hasNext());
        MongoInsertionDto insertionDto = new MongoInsertionDto();
        insertionDto.databaseName = "aDatabase";
        insertionDto.collectionName = "aCollection";
        insertionDto.data = "{\"aField\":\"aString\"}";
        MongoInsertionResultsDto resultsDto = MongoScriptRunner.executeInsert(getConnection(), Collections.singletonList(insertionDto));
        assertTrue(resultsDto.executionResults.get(0));
        assertTrue(connection.getDatabase("aDatabase").getCollection("aCollection").find().cursor().hasNext());
    }

    public Object getConnection() {
        return connection;
    }
}
