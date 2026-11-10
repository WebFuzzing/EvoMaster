package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class MongoCollectionClassReplacementTestBase  {

    protected static MongoClient mongoClient;

    protected static final int MONGODB_PORT = 27017;

    protected static final GenericContainer<?> mongodb = new GenericContainer<>("mongo:6.0")
            .withExposedPorts(MONGODB_PORT);

    protected final static String DATABASE_NAME = "myDatabase";

    protected final static String COLLECTION_NAME = "myCollection";

    protected static @NotNull MongoCollection<Document> getMongoCollection() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        return collection;
    }

    protected static void insertPerson(String name, int age) {
        getMongoCollection().insertOne(new Document("name", name).append("age", age));
    }

    protected static List<Document> verifiedPipeline() {
        return Collections.singletonList(setVerified());
    }

    protected static Document setVerified() {
        return new Document("$set", new Document("verified", true));
    }

    protected BsonDocument assertSingleRecordedCommand(boolean expectedSuccess) {
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


    @BeforeEach
    public void clearDatabase() {

        final MongoCollection<Document> collection = getMongoCollection();

        // delete all documents in collection (if any)
        collection.deleteMany(new Document());

        ExecutionTracer.reset();
    }

}
