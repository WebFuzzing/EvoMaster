package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MongoCollectionPipelineUpdateClassReplacementTest extends MongoCollectionClassReplacementTestBase {

    @Test
    public void testUpdateManyWithClientSessionFilterPipelineAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateMany(collection, clientSession, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

            assertEquals(2, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateManyWithFilterAndPipeline() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateMany(collection, new Document("age", 30), verifiedPipeline());

        assertEquals(2, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateManyWithFilterPipelineAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateMany(collection, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

        assertEquals(2, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateOneWithClientSessionFilterAndPipeline() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateOne(collection, clientSession, new Document("age", 30), verifiedPipeline());

            assertEquals(1, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateOneWithClientSessionFilterPipelineAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateOne(collection, clientSession, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

            assertEquals(1, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateOneWithFilterAndPipeline() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateOne(collection, new Document("age", 30), verifiedPipeline());

        assertEquals(1, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateOneWithFilterPipelineAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionPipelineUpdateClassReplacement.updateOne(collection, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

        assertEquals(1, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }
}
