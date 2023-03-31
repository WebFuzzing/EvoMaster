package org.evomaster.client.java.controller.internal.db;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculator;
import org.evomaster.client.java.instrumentation.MongoInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public MongoHandler() {
        distances = new ArrayList<>();
        operations = new ArrayList<>();
        extractMongoExecution = true;
        calculateHeuristics = true;
    }
    public void reset() {
        operations.clear();
        distances.clear();
    }

    public void handle(MongoInfo info) {
        if (!extractMongoExecution) {
            return;
        }

        operations.add(info);
    }

    public List<MongoOperationDistance> getDistances() {

        operations.stream().filter(info -> info.getQuery() != null).forEach(mongoInfo -> {
            double dist;
            try {
                dist = computeDistance(mongoInfo);
            } catch (Exception e) {
                dist = Double.MAX_VALUE;
            }
            distances.add(new MongoOperationDistance((Bson) mongoInfo.getQuery(), dist));
        });

        operations.clear();

        return distances;
    }

    private double computeDistance(MongoInfo info) {
        MongoCollection<Document> collection = (MongoCollection<Document>) info.getCollection();
        FindIterable<Document> documents = collection.find();

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        double min = Double.MAX_VALUE;

        for (Document doc : documents) {
            double dist = calculator.computeExpression((Bson) info.getQuery(), doc);
            if (dist == 0) return 0;
            if (dist < min) min = dist;
        }
        return min;
    }

    public boolean isCalculateHeuristics() {return calculateHeuristics;}

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
