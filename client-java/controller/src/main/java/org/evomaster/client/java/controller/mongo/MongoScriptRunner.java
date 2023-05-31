package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used to execute Mongo commands
 */
public class MongoScriptRunner {

    /**
     * Default constructor
     */
    public MongoScriptRunner() {
    }

    public static MongoInsertionResultsDto execInsert(Object conn, List<MongoInsertionDto> insertions){

        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        List<Boolean> mongoResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));

        for (int i = 0; i < insertions.size(); i++) {

            MongoInsertionDto insDto = insertions.get(i);

            try {
                Class<?> documentClass = Class.forName("org.bson.Document");
                Object document = documentClass.getDeclaredConstructor().newInstance();
                document = document.getClass().getMethod("parse", String.class).invoke(document, insDto.data.get(0).value);
                Object database = conn.getClass().getMethod("getDatabase", String.class).invoke(conn,insDto.databaseName);
                Object collection = database.getClass().getMethod("getCollection", String.class).invoke(database, insDto.collectionName);
                Class.forName("com.mongodb.client.MongoCollection").getMethod("insertOne", Object.class).invoke(collection, document);
                mongoResults.set(i, true);
            } catch (Exception e) {
                String msg = "Failed to execute insertion with index " + i + " with Mongo. Error: " + e.getMessage();
                throw new RuntimeException(msg, e);
            }
        }

        MongoInsertionResultsDto insertionResultsDto = new MongoInsertionResultsDto();
        insertionResultsDto.executionResults = mongoResults;
        return insertionResultsDto;
    }

}
