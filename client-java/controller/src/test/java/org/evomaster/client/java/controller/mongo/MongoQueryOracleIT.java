package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks the expectations independently of EvoMaster. Uses an isolated database, including when
 * evomaster.mongo.uri points to an already running server. Without that property, starts MongoDB 7.
 */
class MongoQueryOracleIT {
    private static GenericContainer<?> container;
    private static MongoClient client;
    private static MongoDatabase database;
    private MongoCollection<Document> collection;

    @BeforeAll
    static void startMongo() {
        String uri = System.getProperty("evomaster.mongo.uri");
        if (uri == null || uri.isEmpty()) {
            container = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
            container.start();
            uri = "mongodb://" + container.getHost() + ":" + container.getMappedPort(27017);
        }
        client = MongoClients.create(uri);
        database = client.getDatabase("geometry_regressions_" + UUID.randomUUID().toString().replace("-", ""));
        database.runCommand(new Document("ping", 1));
    }

    @BeforeEach
    void clearCollection() {
        collection = database.getCollection("locations");
        collection.drop();
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

    @ParameterizedTest(name = "MongoDB: {0}")
    @MethodSource({"org.evomaster.client.java.controller.mongo.MongoGeometryRegressionCases#scenarios",
            "org.evomaster.client.java.controller.mongo.MongoQueryRegressionCases#scenarios",
            "org.evomaster.client.java.controller.mongo.MongoBsonRegressionCases#scenarios"})
    void shouldConfirmExpectedMatchOnLiveMongo(MongoQueryCase scenario) {
        if (scenario.index() != null) {
            collection.createIndex(scenario.index());
        }
        // The bundled 4.2 driver predates MongoDB 5's support for dollar-prefixed stored keys.
        // Let the live server validate the fixture through its ordinary insert command.
        Document inserted = database.runCommand(new Document("insert", collection.getNamespace().getCollectionName())
                .append("documents", Collections.singletonList(scenario.document())));
        assertFalse(inserted.containsKey("writeErrors"), inserted.toJson());
        assertEquals(1, inserted.getInteger("n").intValue());
        // Use find rather than countDocuments: the latter runs an aggregation that disallows $near.
        assertEquals(scenario.matches ? 1 : 0, collection.find(scenario.query()).into(new ArrayList<>()).size());
    }

    @Test
    void shouldConfirmDetailedLinesAreDisjoint() {
        collection.insertOne(new Document("loc", MongoGeometryRegressionCases.detailedLine(2000, 0)));
        assertEquals(0L, collection.countDocuments(MongoGeometryRegressionCases.geo("$geoIntersects",
                MongoGeometryRegressionCases.detailedLine(2000, 1))));
        assertEquals(1L, collection.countDocuments(MongoGeometryRegressionCases.geo("$geoIntersects",
                MongoGeometryRegressionCases.detailedLine(2000, 0))));
    }
}
