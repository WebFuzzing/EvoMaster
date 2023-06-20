package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.api.dto.database.execution.FailedQuery;
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto;
import org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculator;
import org.evomaster.client.java.controller.mongo.MongoOperation;
import org.evomaster.client.java.controller.mongo.QueryParser;
import org.evomaster.client.java.controller.mongo.operations.*;
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

    private final List<MongoOperation> failedQueries;

    private final HashMap<String, Class<?>> collectionInfo;

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
        // collectionInfo is not cleared to avoid losing the info as it's retrieved when SUT is started
    }

    public void handle(MongoInfo info) {
        if (!extractMongoExecution) {
            return;
        }

        operations.add(info);
    }

    public void handle(MongoCollectionInfo info) {
        if (!extractMongoExecution) {
            return;
        }

        collectionInfo.put(info.getCollectionName(), info.getDocumentsType());
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

            if (dist > 0 ) {
                Object collection = mongoInfo.getCollection();
                failedQueries.add(new MongoOperation(collection, mongoInfo.getQuery()));
            }
        });
        operations.clear();

        return distances;
    }

    private double computeDistance(MongoInfo info) {
        Object collection = info.getCollection();
        Iterable<?> documents = getDocuments(collection);

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

    public MongoExecutionDto getExecutionDto(){
        MongoExecutionDto dto = new MongoExecutionDto();
        dto.failedQueries = failedQueries.stream().map(this::extractRelevantInfo).collect(Collectors.toList());
        return dto;
    }

    private FailedQuery extractRelevantInfo(MongoOperation operation) {
        QueryOperation query = new QueryParser().parse(operation.getQuery());
        Object collection = operation.getCollection();

        Map<String, Class<?>> accessedFields = extractFieldsInQuery(query);
        Class<?> documentsType;

        String collectionName;
        String databaseName;

        try {
            Class<?> collectionClass = collection.getClass().getClassLoader().loadClass("com.mongodb.client.MongoCollection");
            Object namespace = collectionClass.getMethod("getNamespace").invoke(collection);
            collectionName = (String) namespace.getClass().getMethod("getCollectionName").invoke(namespace);
            databaseName = (String) namespace.getClass().getMethod("getDatabaseName").invoke(namespace);
        } catch (ClassNotFoundException |IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
            throw new RuntimeException(e);
        }

        // I should be as specific as I can with the type the collection's documents should have.
        // There are a few ways to get that info:

        // 1) Using collection.getDocumentsClass().
        //    Spring for example "ignore" this and store type info inside repository.

        // 2) When using Spring, retrieving the type of the repository associated with the collection.
        //    The MongoEntityInformation replacement makes this possible.

        // 3) Extract from the query the fields used and type of each of them. This probably won't
        //    work as expected as usually a subset of fields is used in a query.

        if(collectionInfo.containsKey(collectionName)){
            documentsType = collectionInfo.get(collectionName);
        }else{
            documentsType = extractDocumentsType(collection);
        }

        return new FailedQuery(databaseName, collectionName, documentsType, accessedFields);
    }

    private static Class<?> extractDocumentsType(Object collection) {
        try {
            Class<?> collectionClass = collection.getClass().getClassLoader().loadClass("com.mongodb.client.MongoCollection");
            return (Class<?>) collectionClass.getMethod("getDocumentClass").invoke(collection);

        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Class<?>> extractFieldsInQuery(QueryOperation operation) {
        Map<String, Class<?>> accessedFields = new HashMap<>();

        if(operation instanceof ComparisonOperation<?>) {
            ComparisonOperation<?> op  = (ComparisonOperation<?>) operation;
            accessedFields.put(op.getFieldName(), op.getValue().getClass());
        }

        if(operation instanceof AndOperation) {
            AndOperation op  = (AndOperation) operation;
            op.getConditions().forEach(cond -> accessedFields.putAll(extractFieldsInQuery(cond)));
        }

        if(operation instanceof OrOperation) {
            OrOperation op  = (OrOperation) operation;
            op.getConditions().forEach(cond -> accessedFields.putAll(extractFieldsInQuery(cond)));
        }

        if(operation instanceof NorOperation) {
            NorOperation op  = (NorOperation) operation;
            op.getConditions().forEach(cond -> accessedFields.putAll(extractFieldsInQuery(cond)));
        }

        if(operation instanceof InOperation<?>) {
            InOperation<?> op  = (InOperation<?>) operation;
            accessedFields.put(op.getFieldName(), op.getValues().get(0).getClass());
        }

        if(operation instanceof NotInOperation<?>) {
            NotInOperation<?> op  = (NotInOperation<?>) operation;
            accessedFields.put(op.getFieldName(), op.getValues().get(0).getClass());
        }

        if(operation instanceof AllOperation<?>) {
            AllOperation<?> op  = (AllOperation<?>) operation;
            accessedFields.put(op.getFieldName(), op.getValues().getClass());
        }

        if(operation instanceof SizeOperation) {
            SizeOperation op  = (SizeOperation) operation;
            accessedFields.put(op.getFieldName(), op.getValue().getClass());
        }

        if(operation instanceof ExistsOperation) {
            ExistsOperation op  = (ExistsOperation) operation;
            accessedFields.put(op.getFieldName(), null);
        }

        if(operation instanceof ModOperation) {
            ModOperation op  = (ModOperation) operation;
            accessedFields.put(op.getFieldName(),  op.getDivisor().getClass());
        }

        if(operation instanceof TypeOperation) {
            TypeOperation op  = (TypeOperation) operation;
            accessedFields.put(op.getFieldName(), op.getType().getClass());
        }

        /*
        if(operation instanceof ElemMatchOperation) {
            ElemMatchOperation op  = (ElemMatchOperation) operation;
            accessedFields.put(op.getFieldName(), op.getValue());
        }

         */

        return accessedFields;
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
}
