package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.api.dto.database.execution.FailedQuery;
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto;
import org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculator;
import org.evomaster.client.java.controller.mongo.MongoOperation;
import org.evomaster.client.java.instrumentation.MongoCollectionInfo;
import org.evomaster.client.java.instrumentation.MongoInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class used to act upon Mongo commands executed by the SUT
 */
public class MongoHandler {

    /**
     * Info about Find operations executed
     */
    private final List<MongoInfo> operations;

    /**
     * Whether to use execution's info or not
     */
    private volatile boolean extractMongoExecution;

    /**
     * The heuristics based on the Mongo execution
     */
    private final List<MongoOperationDistance> distances;

    /**
     * Whether to calculate heuristics based on execution or not
     */
    private volatile boolean calculateHeuristics;

    /**
     * Unsuccessful executed queries
     */
    private final List<MongoOperation> failedQueries;

    /**
     * Info about types of the documents of collections
     */
    private final Map<String, Class<?>> collectionInfo;

    public MongoHandler() {
        distances = new ArrayList<>();
        operations = new ArrayList<>();
        failedQueries = new ArrayList<>();
        collectionInfo = new HashMap<>();
        extractMongoExecution = true;
        calculateHeuristics = true;
    }

    public void reset() {
        operations.clear();
        distances.clear();
        failedQueries.clear();
        // collectionInfo is not cleared to avoid losing the info as it's retrieved while SUT is starting
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public boolean isExtractMongoExecution() {
        return extractMongoExecution;
    }

    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    public void setExtractMongoExecution(boolean extractMongoExecution) {
        this.extractMongoExecution = extractMongoExecution;
    }

    public void handle(MongoInfo info) {
        if (extractMongoExecution) operations.add(info);
    }

    public void handle(MongoCollectionInfo info) {
        if (extractMongoExecution) collectionInfo.put(info.getCollectionName(), info.getDocumentsType());
    }

    public List<MongoOperationDistance> getDistances() {

        operations.stream().filter(info -> info.getQuery() != null).forEach(mongoInfo -> {
            double dist;
            try {
                dist = computeDistance(mongoInfo);
            } catch (Exception e) {
                dist = Double.MAX_VALUE;
            }
            distances.add(new MongoOperationDistance(mongoInfo.getQuery(), dist));
        });
        operations.clear();

        return distances;
    }

    public MongoExecutionDto getExecutionDto() {
        MongoExecutionDto dto = new MongoExecutionDto();
        dto.failedQueries = failedQueries.stream().map(this::extractRelevantInfo).collect(Collectors.toList());
        return dto;
    }

    private double computeDistance(MongoInfo info) {
        Object collection = info.getCollection();
        Iterable<?> documents = getDocuments(collection);
        boolean collectionIsEmpty = !documents.iterator().hasNext();

        if (collectionIsEmpty) failedQueries.add(new MongoOperation(collection, info.getQuery()));

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        double min = Double.MAX_VALUE;

        for (Object doc : documents) {
            double dist = calculator.computeExpression(info.getQuery(), doc);
            if (dist == 0) return 0;
            if (dist < min) min = dist;
        }
        return min;
    }

    private static Iterable<?> getDocuments(Object collection) {
        try {
            Class<?> collectionClass = collection.getClass().getClassLoader().loadClass("com.mongodb.client.MongoCollection");
            return (Iterable<?>) collectionClass.getMethod("find").invoke(collection);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException("Failed to retrieve all documents from a mongo collection", e);
        }
    }

    private FailedQuery extractRelevantInfo(MongoOperation operation) {
        Object collection = operation.getCollection();

        String databaseName;
        String collectionName;
        Class<?> documentsType;

        try {
            Class<?> collectionClass = collection.getClass().getClassLoader().loadClass("com.mongodb.client.MongoCollection");
            Object namespace = collectionClass.getMethod("getNamespace").invoke(collection);
            databaseName = (String) namespace.getClass().getMethod("getDatabaseName").invoke(namespace);
            collectionName = (String) namespace.getClass().getMethod("getCollectionName").invoke(namespace);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to retrieve collection name or database name", e);
        }

        if (collectionTypeIsRegistered(collectionName)) {
            documentsType = collectionInfo.get(collectionName);
        } else {
            documentsType = extractDocumentsType(collection);
        }

        return new FailedQuery(databaseName, collectionName, documentsType);
    }

    private boolean collectionTypeIsRegistered(String collectionName) {
        return collectionInfo.containsKey(collectionName);
    }

    private static Class<?> extractDocumentsType(Object collection) {
        try {
            Class<?> collectionClass = collection.getClass().getClassLoader().loadClass("com.mongodb.client.MongoCollection");
            return (Class<?>) collectionClass.getMethod("getDocumentClass").invoke(collection);
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException("Failed to retrieve document's type from collection", e);
        }
    }
}
