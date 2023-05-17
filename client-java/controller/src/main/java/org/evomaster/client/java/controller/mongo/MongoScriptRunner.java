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

            /*
            try {
                insDto.data.forEach(field -> conn.getDatabase("persons").getCollection(insDto.collectionName).insertOne(Document.parse(field.value)));
                mongoResults.set(i, true);
            } catch (Exception e) {
                String msg = "Failed to execute insertion with index " + i + " with Mongo. Error: " + e.getMessage();
                throw new RuntimeException(msg, e);
            }

             */
        }

        MongoInsertionResultsDto insertionResultsDto = new MongoInsertionResultsDto();
        insertionResultsDto.executionResults = mongoResults;
        return insertionResultsDto;
    }

}
