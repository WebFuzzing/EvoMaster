package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoQueryException;
import com.mongodb.client.*;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

public class MongoCollectionClassReplacementTest {

    private static MongoClient mongoClient;
    private static final int MONGODB_PORT = 27017;
    private static GenericContainer<?> mongodb;

    @BeforeAll
    public static void initMongoClient() {
        String uri = System.getProperty("evomaster.mongo.uri");
        if (uri == null || uri.isEmpty()) {
            mongodb = new GenericContainer<>("mongo:7.0").withExposedPorts(MONGODB_PORT);
            mongodb.start();
            uri = "mongodb://" + mongodb.getHost() + ":" + mongodb.getMappedPort(MONGODB_PORT);
        }

        CodecRegistry codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()),
                CodecRegistries.fromCodecs(new MongoCollectionTestDtoCodec())
        );

        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        builder.codecRegistry(codecRegistry);
        builder.applyConnectionString(new ConnectionString(uri));
        MongoClientSettings settings = builder
                .build();

        mongoClient = MongoClients.create(settings);

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void resetExecutionTracer() {
        ExecutionTracer.reset();
        try {
            if (mongoClient != null) {
                mongoClient.getDatabase(DATABASE_NAME).drop();
            }
        } finally {
            try {
                if (mongoClient != null) {
                    mongoClient.close();
                }
            } finally {
                if (mongodb != null) {
                    mongodb.stop();
                }
            }
        }
    }

    private final static String DATABASE_NAME = "mongo_replacement_" + UUID.randomUUID().toString().replace("-", "");
    private final static String COLLECTION_NAME = "myCollection";

    @BeforeEach
    public void clearDatabase() {

        final MongoCollection<Document> collection = getMongoCollection();

        // delete all documents in collection (if any)
        collection.deleteMany(new Document());

        ExecutionTracer.reset();
        ExecutionTracer.setExecutingInitMongo(false);
    }

    @AfterEach
    public void clearRecordedCommands() {
        ExecutionTracer.reset();
        getMongoCollection().drop();
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
        Document retrievedQuery = (Document) mongoFindCommand.getQuery();
        assertEquals(30, retrievedQuery.getInteger("age"));

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
        String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(MongoCollectionTestDto.class, true, converters);
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

        Document retrievedQuery = (Document) mongoFindCommand.getQuery();
        assertEquals(17, retrievedQuery.getInteger("age"));

        String documentType = mongoFindCommand.getDocumentsType();
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(MongoCollectionTestDto.class, true, converters);
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

            Document retrievedQuery = (Document) mongoFindCommand.getQuery();
            assertEquals(23, retrievedQuery.getInteger("age"));


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

            Document retrievedQuery = (Document) mongoFindCommand.getQuery();
            assertEquals(23, retrievedQuery.getInteger("age"));


            String documentType = mongoFindCommand.getDocumentsType();
            List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
            String bsonDocumentSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(MongoCollectionTestDto.class, true, converters);
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
        Document retrievedQuery = (Document) mongoFindCommand.getQuery();
        assertTrue(retrievedQuery.get("tags") instanceof Document);
        assertEquals(-1, ((Document) retrievedQuery.get("tags")).get("$size"));

        assertEquals(false, mongoFindCommand.isSuccessfullyExecuted());

    }


    @Test
    public void shouldRecordTheExplicitFindResultClassForDataGeneration() {
        MongoCollection<Document> documents = getMongoCollection();
        documents.insertOne(new Document("username", "alice"));
        assertEquals("alice", documents.find(new Document(), Person.class).first().getUsername(),
                "The actual driver must support the requested result class");
        documents.deleteMany(new Document());

        // A typed collection is the passing control for capturing the Person schema.
        MongoCollection<Person> people = mongoClient.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME, Person.class);
        FindIterable<?> control = (FindIterable<?>) MongoCollectionClassReplacement.find(people, new Document());
        assertNull(control.first());
        MongoFindCommand controlCommand = onlyRecordedCommand();
        assertTrue(controlCommand.isSuccessfullyExecuted());
        String expectedSchema = controlCommand.getDocumentsType();
        assertTrue(expectedSchema.contains("username"), "The control must capture the POJO property");
        ExecutionTracer.reset();

        // On an empty collection this schema is the fallback used to generate matching documents.
        // An explicit {} query keeps this separate from the null-query find() regression.
        FindIterable<?> actual = (FindIterable<?>) MongoCollectionClassReplacement.find(
                documents, new Document(), Person.class);
        assertNull(actual.first());
        MongoFindCommand actualCommand = onlyRecordedCommand();
        assertTrue(actualCommand.isSuccessfullyExecuted());
        assertEquals(expectedSchema, actualCommand.getDocumentsType(),
                "find({}, Person.class) must capture the Person schema just like a Person collection");
    }

    @Test
    public void shouldRecordSuccessfulExecutionAfterProjectionIsConfigured() {
        MongoCollection<Document> documents = getMongoCollection();
        documents.insertOne(new Document("username", "alice").append("age", "not an integer"));
        MongoCollection<Person> people = mongoClient.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME, Person.class);
        Document projection = new Document("age", 0);
        assertEquals("alice", people.find(new Document()).projection(projection).first().getUsername(),
                "Excluding the incompatible field must produce a valid POJO result");

        FindIterable<?> result = (FindIterable<?>) MongoCollectionClassReplacement.find(people, new Document());
        Person actual = (Person) result.projection(projection).first();
        assertNotNull(actual);
        assertEquals("alice", actual.getUsername(), "The instrumented query also completed successfully");

        assertTrue(onlyRecordedCommand().isSuccessfullyExecuted(),
                "The preliminary unprojected decode must not mark the successful projected query as failed");
    }

    private MongoFindCommand onlyRecordedCommand() {
        List<MongoFindCommand> commands = ExecutionTracer.exposeAdditionalInfoList().stream()
                .flatMap(info -> info.getMongoInfoData().stream())
                .collect(Collectors.toList());
        assertEquals(1, commands.size());
        return commands.get(0);
    }

    public static class Person {
        private String username;
        private int age;

        public Person() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
