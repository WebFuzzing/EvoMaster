package org.evomaster.client.java.controller.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.MongoCollectionClassReplacement;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises actual find replacements against MongoDB, including the recorded execution metadata. */
public class MongoInstrumentationRegressionIT {
    private static GenericContainer<?> container;
    private static MongoClient client;
    private static MongoDatabase database;

    @BeforeAll
    static void startMongo() {
        String uri = System.getProperty("evomaster.mongo.uri");
        if (uri == null || uri.isEmpty()) {
            container = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
            container.start();
            uri = "mongodb://" + container.getHost() + ":" + container.getMappedPort(27017);
        }
        client = MongoClients.create(uri);
        database = client.getDatabase("instrumentation_regressions_" + UUID.randomUUID().toString().replace("-", ""))
                .withCodecRegistry(CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())));
        database.runCommand(new Document("ping", 1));
    }

    @BeforeEach
    void resetRecordedCommands() {
        ExecutionTracer.reset();
    }

    @AfterEach
    void clearRecordedCommands() {
        ExecutionTracer.reset();
        database.getCollection("people").drop();
        database.getCollection("projected_people").drop();
    }

    @AfterAll
    static void stopMongo() {
        try {
            if (database != null) {
                database.drop();
            }
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } finally {
                if (container != null) {
                    container.stop();
                }
            }
        }
    }

    @Test
    void shouldRecordTheExplicitFindResultClassForDataGeneration() {
        MongoCollection<Document> documents = database.getCollection("people");
        documents.insertOne(new Document("username", "alice"));
        assertEquals("alice", documents.find(new Document(), Person.class).first().getUsername(),
                "The actual driver must support the requested result class");
        documents.deleteMany(new Document());

        // A typed collection is the passing control for capturing the Person schema.
        MongoCollection<Person> people = database.getCollection("people", Person.class);
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
    void shouldRecordSuccessfulExecutionAfterProjectionIsConfigured() {
        MongoCollection<Document> documents = database.getCollection("projected_people");
        documents.insertOne(new Document("username", "alice").append("age", "not an integer"));
        MongoCollection<Person> people = database.getCollection("projected_people", Person.class);
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
