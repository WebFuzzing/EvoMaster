package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
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
