package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
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
    public MongoScriptRunner() {}

    /**
     * Execute the different Mongo insertions.
     *
     * @param connection a connection to the database (MongoClient)
     * @param insertions the Mongo insertions to execute
     * @return a MongoInsertionResultsDto
     */
    public static MongoInsertionResultsDto executeInsert(Object connection, List<MongoInsertionDto> insertions) {

        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        List<Boolean> mongoResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));

        for (int i = 0; i < insertions.size(); i++) {

            MongoInsertionDto insertionDto = insertions.get(i);

            try {
                Object document = parseEJSON(insertionDto.data);
                insertDocument(connection, insertionDto.databaseName, insertionDto.collectionName, document);
                mongoResults.set(i, true);
                SimpleLogger.debug(insertionDto.data + " inserted into database: " + insertionDto.databaseName + " and collection: " + insertionDto.collectionName);
            } catch (Exception e) {
                final String errorMessage;
                if (e instanceof  InvocationTargetException) {
                    InvocationTargetException  invocationTargetException = (InvocationTargetException)e;
                    Throwable innerException = invocationTargetException.getTargetException();
                    errorMessage = innerException.getMessage();
                } else {
                    errorMessage = e.getMessage();
                }
                String msg = "Failed to execute insertion with index " + i + " with Mongo. Error: " + errorMessage;
                throw new RuntimeException(msg, e);
            }
        }

        MongoInsertionResultsDto insertionResultsDto = new MongoInsertionResultsDto();
        insertionResultsDto.executionResults = mongoResults;
        return insertionResultsDto;
    }

    private static void insertDocument(Object connection, String databaseName, String collectionName, Object document) throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Object database = connection.getClass().getMethod("getDatabase", String.class).invoke(connection, databaseName);
        Object collection = database.getClass().getMethod("getCollection", String.class).invoke(database, collectionName);
        Class.forName("com.mongodb.client.MongoCollection").getMethod("insertOne", Object.class).invoke(collection, document);
    }

    private static Object parseEJSON(String documentAsEJSON) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> documentClass = Class.forName("org.bson.Document");
        Object document = Class.forName("org.bson.Document").getDeclaredConstructor().newInstance();
        document = documentClass.getMethod("parse", String.class).invoke(document, documentAsEJSON);
        return document;
    }

}
