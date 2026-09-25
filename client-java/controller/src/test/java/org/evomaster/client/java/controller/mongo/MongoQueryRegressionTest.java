package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MongoQueryRegressionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource({"org.evomaster.client.java.controller.mongo.MongoGeometryRegressionCases#scenarios",
            "org.evomaster.client.java.controller.mongo.MongoQueryRegressionCases#scenarios"})
    void shouldAgreeWithVerifiedMongoMatch(MongoQueryCase scenario) {
        MongoDistanceWithMetrics result = assertDoesNotThrow(() -> new MongoHeuristicsCalculator()
                .computeDistanceDocuments(scenario.query(), Collections.singletonList(scenario.document())));

        assertAll(
                () -> assertEquals(scenario.matches, result.mongoDistance == 0.0,
                        "Zero distance must mean that MongoDB matches the document"),
                () -> assertTrue(Double.isFinite(result.mongoDistance)
                        && result.mongoDistance >= 0.0 && result.mongoDistance <= 1.0),
                () -> assertEquals(1, result.numberOfEvaluatedDocuments)
        );
    }
}
