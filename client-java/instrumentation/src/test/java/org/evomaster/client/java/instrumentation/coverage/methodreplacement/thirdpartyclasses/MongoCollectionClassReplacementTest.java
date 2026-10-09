package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoQueryException;
import com.mongodb.client.*;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.DeleteResult;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.CustomTypeToOasConverter;
import org.evomaster.client.java.instrumentation.object.GeoJsonPointToOasConverter;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

public class MongoCollectionClassReplacementTest {

    private static MongoClient mongoClient;
    private static final int MONGODB_PORT = 27017;
    private static final GenericContainer<?> mongodb = new GenericContainer<>("mongo:6.0")
            .withExposedPorts(MONGODB_PORT);

    @BeforeAll
    public static void initMongoClient() {
        mongodb.start();
        int port = mongodb.getMappedPort(MONGODB_PORT);

        CodecRegistry codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()),
                CodecRegistries.fromCodecs(new MongoCollectionTestDtoCodec())
        );

        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        builder.codecRegistry(codecRegistry);
        builder.applyConnectionString(new ConnectionString("mongodb://localhost:" + port + "/" + "aDatabase"));
        MongoClientSettings settings = builder
                .build();

        mongoClient = MongoClients.create(settings);

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    private final static String DATABASE_NAME = "myDatabase";
    private final static String COLLECTION_NAME = "myCollection";

    @BeforeEach
    public void clearDatabase() {

        final MongoCollection<Document> collection = getMongoCollection();

        // delete all documents in collection (if any)
        collection.deleteMany(new Document());

        ExecutionTracer.reset();
    }


    @Test
    public void testFindOnly() {
        final MongoCollection<Document> collection = getMongoCollection();
        ExecutionTracer.setExecutingInitMongo(false);
        FindIterable<?> findIterable = (FindIterable<?>) MongoCollectionClassReplacement.find(collection);

        MongoCursor<?> cursor = findIterable.iterator();
        assertFalse(cursor.hasNext());
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
        assertEquals(1, mongoFindCommands.size());

        MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
        assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
        assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
        assertNull(mongoFindCommand.getQuery());
        String documentType = mongoFindCommand.getDocumentsType();
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(Document.class, true, converters);
        assertEquals(bsonDocumentSchema, documentType);
    }

    @Test
    public void testFindWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();

        Document documentJohnDoe = new Document("name", "John Doe")
                .append("age", 30)
                .append("city", "New York");
        collection.insertOne(documentJohnDoe);

        Document documentJaneDoe = new Document("name", "Jane Doe")
                .append("age", 25)
                .append("city", "Chicago");
        collection.insertOne(documentJaneDoe);

        Document ageFilter = new Document("age", 30);

        ExecutionTracer.setExecutingInitMongo(false);
        FindIterable<?> findIterable = (FindIterable<?>) MongoCollectionClassReplacement.find(collection, ageFilter);

        MongoCursor<?> cursor = findIterable.iterator();
        assertTrue(cursor.hasNext());

        Document retrievedDocument = (Document) cursor.next();
        assertEquals("John Doe", retrievedDocument.getString("name"));
        assertEquals(30, retrievedDocument.getInteger("age"));
        assertEquals("New York", retrievedDocument.getString("city"));

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
        assertEquals(1, mongoFindCommands.size());

        MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
        assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
        assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
        assertNotNull(mongoFindCommand.getQuery());
        BsonDocument retrievedQuery = (BsonDocument) mongoFindCommand.getQuery();
        assertEquals(30, retrievedQuery.getInt32("age").getValue());

        String documentType = mongoFindCommand.getDocumentsType();
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(Document.class, true, converters);
        assertEquals(bsonDocumentSchema, documentType);
    }


    @Test
    public void testFindWithResultClass() {
        final MongoCollection<Document> collection = getMongoCollection();
        MongoCollectionTestDto dto = new MongoCollectionTestDto();
        dto.age = 27;
        dto.city = "Washington";
        dto.name = "Charles Doe";

        MongoCollectionTestDtoCodec codec = new MongoCollectionTestDtoCodec();

        // Create BSON
        BsonDocument bsonDocument = new BsonDocument();
        BsonWriter writer = new BsonDocumentWriter(bsonDocument);
        codec.encode(writer, dto, EncoderContext.builder().build());

        // Convert BsonDocument to JSON string
        String json = bsonDocument.toJson();

        // Parse JSON string into Document
        Document document = Document.parse(json);

        collection.insertOne(document);

        ExecutionTracer.setExecutingInitMongo(false);
        FindIterable<MongoCollectionTestDto> findIterable = (FindIterable<MongoCollectionTestDto>) MongoCollectionClassReplacement.find(collection, MongoCollectionTestDto.class);

        MongoCursor<MongoCollectionTestDto> cursor = findIterable.iterator();
        assertTrue(cursor.hasNext());

        MongoCollectionTestDto retrievedInstance = cursor.next();

        assertEquals(27, retrievedInstance.age);
        assertEquals("Washington", retrievedInstance.city);
        assertEquals("Charles Doe", retrievedInstance.name);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
        assertEquals(1, mongoFindCommands.size());

        MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
        assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
        assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
        assertNull(mongoFindCommand.getQuery());

        String documentType = mongoFindCommand.getDocumentsType();
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(Document.class, true, converters);
        assertEquals(bsonDocumentSchema, documentType);
    }

    private static @NotNull MongoCollection<Document> getMongoCollection() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        return collection;
    }

    @Test
    public void testFindWithFilterAndResultClass() {
        final MongoCollection<Document> collection = getMongoCollection();

        Document filter = new Document("age", 17);

        ExecutionTracer.setExecutingInitMongo(false);
        FindIterable<MongoCollectionTestDto> findIterable = (FindIterable<MongoCollectionTestDto>) MongoCollectionClassReplacement.find(collection, filter, MongoCollectionTestDto.class);

        MongoCursor<MongoCollectionTestDto> cursor = findIterable.iterator();
        assertFalse(cursor.hasNext());

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
        assertEquals(1, mongoFindCommands.size());

        MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
        assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
        assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
        assertNotNull(mongoFindCommand.getQuery());

        BsonDocument retrievedQuery = (BsonDocument) mongoFindCommand.getQuery();
        assertEquals(17, retrievedQuery.getInt32("age").getValue());

        String documentType = mongoFindCommand.getDocumentsType();
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(Document.class, true, converters);
        assertEquals(bsonDocumentSchema, documentType);
    }


    @Test
    public void testFindWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();

            Document filter = new Document("age", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            FindIterable<?> findIterable = (FindIterable<?>) MongoCollectionClassReplacement.find(collection, clientSession, filter);

            MongoCursor<?> cursor = findIterable.iterator();
            assertFalse(cursor.hasNext());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
            assertEquals(1, mongoFindCommands.size());

            MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
            assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
            assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
            assertNotNull(mongoFindCommand.getQuery());

            BsonDocument retrievedQuery = (BsonDocument) mongoFindCommand.getQuery();
            assertEquals(23, retrievedQuery.getInt32("age").getValue());


            String documentType = mongoFindCommand.getDocumentsType();
            List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
            String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(Document.class, true, converters);
            assertEquals(bsonDocumentSchema, documentType);

        }
    }

    @Test
    public void testFindWithClientSessionAndFilterAndResultClass() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();

            Document filter = new Document("age", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            FindIterable<MongoCollectionTestDto> findIterable = (FindIterable<MongoCollectionTestDto>) MongoCollectionClassReplacement.find(collection, clientSession, filter, MongoCollectionTestDto.class);

            MongoCursor<?> cursor = findIterable.iterator();
            assertFalse(cursor.hasNext());

            List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfoList.size());
            Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
            assertEquals(1, mongoFindCommands.size());

            MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
            assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
            assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
            assertNotNull(mongoFindCommand.getQuery());

            BsonDocument retrievedQuery = (BsonDocument) mongoFindCommand.getQuery();
            assertEquals(23, retrievedQuery.getInt32("age").getValue());


            String documentType = mongoFindCommand.getDocumentsType();
            List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
            String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(Document.class, true, converters);
            assertEquals(bsonDocumentSchema, documentType);

        }
    }


    @Test
    public void testFindWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();

        Document tags = new Document("tags", Collections.emptyList());
        collection.insertOne(tags);

        Document tagsFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        FindIterable<?> findIterable = (FindIterable<?>) MongoCollectionClassReplacement.find(collection, tagsFilter);

        try {
            findIterable.iterator();
            fail("Expected an exception to be thrown due to invalid filter");
        } catch (MongoQueryException ex) {
            // expected exception due to invalid filter
        }

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
        assertEquals(1, mongoFindCommands.size());

        MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
        assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
        assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
        assertNotNull(mongoFindCommand.getQuery());
        BsonDocument retrievedQuery = (BsonDocument) mongoFindCommand.getQuery();
        assertTrue(retrievedQuery.get("tags") instanceof BsonDocument);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());

        assertEquals(false, mongoFindCommand.isSuccessfullyExecuted());

    }

    private BsonDocument assertSingleRecordedCommand(boolean expectedSuccess) {
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<MongoFindCommand> mongoFindCommands = additionalInfoList.get(0).getMongoInfoData();
        assertEquals(1, mongoFindCommands.size());

        MongoFindCommand mongoFindCommand = mongoFindCommands.iterator().next();
        assertEquals(COLLECTION_NAME, mongoFindCommand.getCollectionName());
        assertEquals(DATABASE_NAME, mongoFindCommand.getDatabaseName());
        assertEquals(expectedSuccess, mongoFindCommand.isSuccessfullyExecuted());
        assertNotNull(mongoFindCommand.getQuery());
        return (BsonDocument) mongoFindCommand.getQuery();
    }

    private void insertPerson(String name, int age) {
        getMongoCollection().insertOne(new Document("name", name).append("age", age));
    }

    @Test
    public void testCountDocumentsWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        long count = MongoCollectionClassReplacement.countDocuments(collection, new Document("age", 30));

        assertEquals(1, count);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testCountDocumentsWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        long count = MongoCollectionClassReplacement.countDocuments(collection, new Document("age", 99));

        assertEquals(0, count);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testCountDocumentsWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);

        ExecutionTracer.setExecutingInitMongo(false);
        long count = MongoCollectionClassReplacement.countDocuments(collection, new Document("age", 30), new com.mongodb.client.model.CountOptions().limit(1));

        assertEquals(1, count);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testCountDocumentsWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            long count = MongoCollectionClassReplacement.countDocuments_EM_0(collection, clientSession, new Document("age", 23));

            assertEquals(1, count);
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testCountDocumentsWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            long count = MongoCollectionClassReplacement.countDocuments(collection, clientSession, new Document("age", 23), new com.mongodb.client.model.CountOptions().limit(1));

            assertEquals(1, count);
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testCountDocumentsWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.countDocuments(collection, invalidFilter));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    @Test
    public void testDeleteOneWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteOne(collection, new Document("age", 30));

        assertEquals(1, result.getDeletedCount());
        assertEquals(1, collection.countDocuments());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testDeleteOneWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteOne(collection, new Document("age", 99));

        assertEquals(0, result.getDeletedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testDeleteOneWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        ExecutionTracer.setExecutingInitMongo(false);
        DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteOne(collection, new Document("age", 30), new DeleteOptions());

        assertEquals(1, result.getDeletedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testDeleteOneWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteOne_EM_0(collection, clientSession, new Document("age", 23));

            assertEquals(1, result.getDeletedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testDeleteOneWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteOne(collection, clientSession, new Document("age", 23), new DeleteOptions());

            assertEquals(1, result.getDeletedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testDeleteOneWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.deleteOne(collection, invalidFilter));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    @Test
    public void testDeleteManyWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteMany(collection, new Document("age", 30));

        assertEquals(1, result.getDeletedCount());
        assertEquals(1, collection.countDocuments());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testDeleteManyWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteMany(collection, new Document("age", 99));

        assertEquals(0, result.getDeletedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testDeleteManyWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        ExecutionTracer.setExecutingInitMongo(false);
        DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteMany(collection, new Document("age", 30), new DeleteOptions());

        assertEquals(1, result.getDeletedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testDeleteManyWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteMany_EM_0(collection, clientSession, new Document("age", 23));

            assertEquals(1, result.getDeletedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testDeleteManyWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            DeleteResult result = (DeleteResult) MongoCollectionClassReplacement.deleteMany(collection, clientSession, new Document("age", 23), new DeleteOptions());

            assertEquals(1, result.getDeletedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testDeleteManyWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.deleteMany(collection, invalidFilter));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    private static Document setVerified() {
        return new Document("$set", new Document("verified", true));
    }

    private static List<Document> verifiedPipeline() {
        return Collections.singletonList(setVerified());
    }

    @Test
    public void testUpdateOneWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, new Document("age", 30), setVerified());

        assertEquals(1, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateOneWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, new Document("age", 30), setVerified(), new UpdateOptions());

        assertEquals(1, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateOneWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne_EM_0(collection, clientSession, new Document("age", 30), setVerified());

            assertEquals(1, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateOneWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, clientSession, new Document("age", 30), setVerified(), new UpdateOptions());

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
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, new Document("age", 30), verifiedPipeline());

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
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

        assertEquals(1, result.getMatchedCount());
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
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, clientSession, new Document("age", 30), verifiedPipeline());

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
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, clientSession, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

            assertEquals(1, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateOneWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateOne(collection, new Document("age", 99), setVerified());

        assertEquals(0, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateOneWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.updateOne(collection, invalidFilter, setVerified()));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    @Test
    public void testUpdateManyWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, new Document("age", 30), setVerified());

        assertEquals(2, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateManyWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, new Document("age", 30), setVerified(), new UpdateOptions());

        assertEquals(2, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateManyWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany_EM_0(collection, clientSession, new Document("age", 30), setVerified());

            assertEquals(2, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateManyWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, clientSession, new Document("age", 30), setVerified(), new UpdateOptions());

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
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, new Document("age", 30), verifiedPipeline());

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
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

        assertEquals(2, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateManyWithClientSessionFilterAndPipeline() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, clientSession, new Document("age", 30), verifiedPipeline());

            assertEquals(2, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateManyWithClientSessionFilterPipelineAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jane Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, clientSession, new Document("age", 30), verifiedPipeline(), new UpdateOptions());

            assertEquals(2, result.getMatchedCount());
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testUpdateManyWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        UpdateResult result = (UpdateResult) MongoCollectionClassReplacement.updateMany(collection, new Document("age", 99), setVerified());

        assertEquals(0, result.getMatchedCount());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testUpdateManyWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.updateMany(collection, invalidFilter, setVerified()));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    @Test
    public void testFindOneAndDeleteWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jane Doe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document deleted = (Document) MongoCollectionClassReplacement.findOneAndDelete(collection, new Document("age", 30));

        assertNotNull(deleted);
        assertEquals("John Doe", deleted.getString("name"));
        assertEquals(1, collection.countDocuments());
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndDeleteWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        Document deleted = (Document) MongoCollectionClassReplacement.findOneAndDelete(collection, new Document("age", 99));

        assertNull(deleted);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndDeleteWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        ExecutionTracer.setExecutingInitMongo(false);
        Document deleted = (Document) MongoCollectionClassReplacement.findOneAndDelete(collection, new Document("age", 30), new FindOneAndDeleteOptions());

        assertNotNull(deleted);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndDeleteWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            Document deleted = (Document) MongoCollectionClassReplacement.findOneAndDelete_EM_0(collection, clientSession, new Document("age", 23));

            assertNotNull(deleted);
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndDeleteWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 23);

            ExecutionTracer.setExecutingInitMongo(false);
            Document deleted = (Document) MongoCollectionClassReplacement.findOneAndDelete(collection, clientSession, new Document("age", 23), new FindOneAndDeleteOptions());

            assertNotNull(deleted);
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(23, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndDeleteWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.findOneAndDelete(collection, invalidFilter));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    @Test
    public void testFindOneAndUpdateWithFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, new Document("age", 30), setVerified());

        assertNotNull(found);
        assertEquals("John Doe", found.getString("name"));
        assertEquals(1, collection.countDocuments(new Document("verified", true)));
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndUpdateWithFilterAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, new Document("age", 30), setVerified(), new FindOneAndUpdateOptions());

        assertNotNull(found);
        assertEquals("John Doe", found.getString("name"));
        assertEquals(1, collection.countDocuments(new Document("verified", true)));
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndUpdateWithClientSessionAndFilter() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate_EM_0(collection, clientSession, new Document("age", 30), setVerified());

            assertNotNull(found);
            assertEquals("John Doe", found.getString("name"));
            assertEquals(1, collection.countDocuments(new Document("verified", true)));
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndUpdateWithClientSessionFilterAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, clientSession, new Document("age", 30), setVerified(), new FindOneAndUpdateOptions());

            assertNotNull(found);
            assertEquals("John Doe", found.getString("name"));
            assertEquals(1, collection.countDocuments(new Document("verified", true)));
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndUpdateWithFilterAndPipeline() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, new Document("age", 30), verifiedPipeline());

        assertNotNull(found);
        assertEquals("John Doe", found.getString("name"));
        assertEquals(1, collection.countDocuments(new Document("verified", true)));
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndUpdateWithFilterPipelineAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, new Document("age", 30), verifiedPipeline(), new FindOneAndUpdateOptions());

        assertNotNull(found);
        assertEquals("John Doe", found.getString("name"));
        assertEquals(1, collection.countDocuments(new Document("verified", true)));
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndUpdateWithClientSessionFilterAndPipeline() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, clientSession, new Document("age", 30), verifiedPipeline());

            assertNotNull(found);
            assertEquals("John Doe", found.getString("name"));
            assertEquals(1, collection.countDocuments(new Document("verified", true)));
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndUpdateWithClientSessionFilterPipelineAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, clientSession, new Document("age", 30), verifiedPipeline(), new FindOneAndUpdateOptions());

            assertNotNull(found);
            assertEquals("John Doe", found.getString("name"));
            assertEquals(1, collection.countDocuments(new Document("verified", true)));
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndUpdateWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndUpdate(collection, new Document("age", 99), setVerified());

        assertNull(found);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndUpdateWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.findOneAndUpdate(collection, invalidFilter, setVerified()));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }

    private static Document replacement() {
        return new Document("name", "Replaced").append("age", 30);
    }

    @Test
    public void testFindOneAndReplaceWithFilterAndReplacement() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndReplace(collection, new Document("age", 30), replacement());

        // by default, the document before the replacement is returned
        assertNotNull(found);
        assertEquals("John Doe", found.getString("name"));
        assertEquals(1, collection.countDocuments(new Document("name", "Replaced")));
        assertEquals(0, collection.countDocuments(new Document("name", "John Doe")));
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndReplaceWithFilterReplacementAndOptions() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);
        insertPerson("Jim Roe", 25);

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndReplace_EM_0(collection, new Document("age", 30), replacement(), new FindOneAndReplaceOptions());

        // by default, the document before the replacement is returned
        assertNotNull(found);
        assertEquals("John Doe", found.getString("name"));
        assertEquals(1, collection.countDocuments(new Document("name", "Replaced")));
        assertEquals(0, collection.countDocuments(new Document("name", "John Doe")));
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(30, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndReplaceWithClientSessionFilterAndReplacement() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            Document found = (Document) MongoCollectionClassReplacement.findOneAndReplace(collection, clientSession, new Document("age", 30), replacement());

            // by default, the document before the replacement is returned
            assertNotNull(found);
            assertEquals("John Doe", found.getString("name"));
            assertEquals(1, collection.countDocuments(new Document("name", "Replaced")));
            assertEquals(0, collection.countDocuments(new Document("name", "John Doe")));
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndReplaceWithClientSessionFilterReplacementAndOptions() {
        try (ClientSession clientSession = mongoClient.startSession()) {
            final MongoCollection<Document> collection = getMongoCollection();
            insertPerson("John Doe", 30);
            insertPerson("Jim Roe", 25);

            ExecutionTracer.setExecutingInitMongo(false);
            Document found = (Document) MongoCollectionClassReplacement.findOneAndReplace(collection, clientSession, new Document("age", 30), replacement(), new FindOneAndReplaceOptions());

            // by default, the document before the replacement is returned
            assertNotNull(found);
            assertEquals("John Doe", found.getString("name"));
            assertEquals(1, collection.countDocuments(new Document("name", "Replaced")));
            assertEquals(0, collection.countDocuments(new Document("name", "John Doe")));
            BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
            assertEquals(30, retrievedQuery.getInt32("age").getValue());
        }
    }

    @Test
    public void testFindOneAndReplaceWithFilterNoMatch() {
        final MongoCollection<Document> collection = getMongoCollection();

        ExecutionTracer.setExecutingInitMongo(false);
        Document found = (Document) MongoCollectionClassReplacement.findOneAndReplace(collection, new Document("age", 99), replacement());

        assertNull(found);
        BsonDocument retrievedQuery = assertSingleRecordedCommand(true);
        assertEquals(99, retrievedQuery.getInt32("age").getValue());
    }

    @Test
    public void testFindOneAndReplaceWithInvalidFilter() {
        final MongoCollection<Document> collection = getMongoCollection();
        insertPerson("John Doe", 30);

        Document invalidFilter = new Document("tags", new Document("$size", -1));

        ExecutionTracer.setExecutingInitMongo(false);
        assertThrows(com.mongodb.MongoException.class, () -> MongoCollectionClassReplacement.findOneAndReplace(collection, invalidFilter, replacement()));

        BsonDocument retrievedQuery = assertSingleRecordedCommand(false);
        assertEquals(-1, retrievedQuery.getDocument("tags").getInt32("$size").getValue());
    }
}
