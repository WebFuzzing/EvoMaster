package org.evomaster.client.java.controller.internal.db.mongo;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.evomaster.client.java.controller.internal.db.PairCommandDistance;
import org.evomaster.client.java.instrumentation.mongo.MongoFindOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class MongoHandler {

    /**
     * Computing heuristics on MongoDB is expensive, as we need to run
     * further queries. So, we buffer them, and execute them only
     * if needed (ie, lazy initialization)
     */
    private final List<MongoFindOperation> buffer;

    private MongoClient mongoClient;

    public MongoHandler() {
        buffer = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();

        calculateHeuristics = true;
    }

    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public void reset() {
        buffer.clear();
        distances.clear();
    }

    public void handle(String mongoOperation) {
        Objects.requireNonNull(mongoOperation);


        if (!calculateHeuristics && !extractMongoExecution) {
            return;
        }

        MongoFindOperation op = new Gson().fromJson(mongoOperation, MongoFindOperation.class);
        buffer.add(op);
    }

    /**
     * The heuristics based on the MongoDB queries
     */
    private final List<PairCommandDistance> distances;

    private volatile boolean calculateHeuristics;

    private volatile boolean extractMongoExecution;

    public List<PairCommandDistance> getDistances() {

        if (mongoClient == null || !calculateHeuristics) {
            return distances;
        }


        buffer.stream()
                .forEach(mongoOperation -> {
                    /*
                        Note: even if the Connection we got to analyze
                        the DB is using MongoLogger, that would not be a problem,
                        as output Mongo operation would not end up on the buffer instance
                        we are iterating on (copy on write), and we clear
                        the buffer after this loop.
                     */
                    if (mongoOperation instanceof MongoFindOperation) {
                        double dist = computeDistance(mongoOperation);
                        distances.add(new PairCommandDistance(mongoOperation.toString(), dist));
                    }
                });
        //side effects on buffer is not important, as it is just a cache
        buffer.clear();

        return distances;
    }

    private double computeDistance(MongoFindOperation op) {
        MongoCollection<Document> mongoCollection = mongoClient.getDatabase(op.getDatabaseName()).getCollection(op.getCollectionName());
        CodecRegistry codecRegistry = mongoCollection.getCodecRegistry();
        BsonDocument query = op.getQuery().toBsonDocument(BsonDocument.class, codecRegistry);
        boolean hasNext = mongoCollection.find(query).iterator().hasNext();
        if (hasNext) {
            return 0;
        } else {
            return Double.MAX_VALUE;
        }
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
