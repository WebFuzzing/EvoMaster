package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MAX_VALUE;
import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MIN_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RedisHandlerTest {

    private RedisHandler handler;

    @BeforeEach
    void setup() {
        handler = new RedisHandler();
        handler.setCalculateHeuristics(true);
    }

    @Test
    void testHandleStoresCommands() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:1"},
                true,
                5
        );
        handler.handle(cmd);

        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();

        assertNotNull(evals);
        assertFalse(evals.isEmpty());
    }

    @Test
    void testResetClearsOperations() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:1"},
                true,
                5
        );
        handler.handle(cmd);
        handler.reset();

        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();
        assertTrue(evals.isEmpty());
    }

    @Test
    void testResetClearsFailedCommands() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                new String[]{"key<user:1>"},
                true,
                5
        );
        handler.handle(cmd);
        handler.getEvaluatedRedisCommands();

        assertFalse(handler.getExecutionDto().failedCommands.isEmpty());

        handler.reset();

        assertTrue(handler.getExecutionDto().failedCommands.isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // FT.SEARCH / FT.AGGREGATE: how the candidate documents of an index are built
    // ---------------------------------------------------------------------------------------

    private static final String INDEX = "idx:products";

    private ReflectionBasedRedisClient clientWith(List<String> indexPrefixes, Map<String, Map<String, String>> hashes) {
        ReflectionBasedRedisClient client = mock(ReflectionBasedRedisClient.class);
        when(client.getIndexPrefixes(INDEX)).thenReturn(indexPrefixes);
        when(client.getKeysByType("hash")).thenReturn(new HashSet<>(hashes.keySet()));
        hashes.forEach((key, fields) -> when(client.getHashFields(key)).thenReturn(fields));
        handler.setRedisClient(client);
        return client;
    }

    private RedisCommandEvaluation evaluateSingle(RedisCommand cmd) {
        handler.handle(cmd);
        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();
        assertEquals(1, evals.size());
        return evals.get(0);
    }

    private static RedisCommand ftSearch(String query) {
        return new RedisCommand(RedisCommand.RedisCommandType.FT_SEARCH, new String[]{INDEX, query}, true, 1);
    }

    private static Map<String, Map<String, String>> hashes(String... keysAndTitles) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndTitles.length; i += 2) {
            result.put(keysAndTitles[i], Collections.singletonMap("title", keysAndTitles[i + 1]));
        }
        return result;
    }

    @Test
    void testFtSearchOnlyEvaluatesHashKeysCoveredByIndexPrefix() {
        clientWith(Collections.singletonList("product:"),
                hashes("product:1", "redis handbook", "product:2", "mongodb handbook", "other:1", "redis"));

        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("@title:redis")).getRedisDistanceWithMetrics();

        assertEquals(2, metrics.getNumberOfEvaluatedKeys(), "other:1 is outside the index prefix");
        assertEquals(H_MIN_VALUE, metrics.getDistance(), 1e-6);
    }

    @Test
    void testFtSearchDoesNotUseDocumentsOutsideThePrefixToSatisfyTheQuery() {
        clientWith(Collections.singletonList("product:"),
                hashes("product:1", "mongodb handbook", "other:1", "redis"));

        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("@title:redis")).getRedisDistanceWithMetrics();

        assertEquals(1, metrics.getNumberOfEvaluatedKeys());
        assertTrue(metrics.getDistance() > H_MIN_VALUE,
                "The only document matching the query is not indexed, so the search cannot succeed");
    }

    @Test
    void testFtSearchAcceptsKeysMatchingAnyOfTheDeclaredPrefixes() {
        clientWith(Arrays.asList("product:", "book:"),
                hashes("product:1", "a", "book:1", "b", "other:1", "c"));

        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("*")).getRedisDistanceWithMetrics();

        assertEquals(2, metrics.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtSearchWithEmptyPrefixCoversEveryHashKey() {
        // an index created without PREFIX reports a single empty prefix and covers the whole keyspace
        clientWith(Collections.singletonList(""), hashes("product:1", "a", "other:1", "b"));

        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("*")).getRedisDistanceWithMetrics();

        assertEquals(2, metrics.getNumberOfEvaluatedKeys());
        assertEquals(H_MIN_VALUE, metrics.getDistance(), 1e-6);
    }

    @Test
    void testFtSearchOnUnknownIndexHasNoCandidates() {
        clientWith(Collections.emptyList(), hashes("product:1", "redis"));

        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("*")).getRedisDistanceWithMetrics();

        assertEquals(0, metrics.getNumberOfEvaluatedKeys());
        assertTrue(metrics.getDistance() > H_MIN_VALUE);
    }

    @Test
    void testFtSearchAsksTheClientForTheIndexInTheCommandAndOnlyForHashKeys() {
        ReflectionBasedRedisClient client = clientWith(Collections.singletonList("product:"), hashes("product:1", "a"));

        evaluateSingle(ftSearch("*"));

        verify(client).getIndexPrefixes(INDEX);
        verify(client).getKeysByType("hash");
        verify(client, never()).getKeysByType("set");
        verify(client, never()).getKeysByType("string");
    }

    @Test
    void testFtAggregateBuildsTheSameCandidatesAsFtSearch() {
        clientWith(Collections.singletonList("product:"),
                hashes("product:1", "redis handbook", "other:1", "redis"));

        RedisCommand aggregate = new RedisCommand(RedisCommand.RedisCommandType.FT_AGGREGATE,
                new String[]{INDEX, "*", "GROUPBY", "1", "@title"}, true, 1);
        RedisDistanceWithMetrics metrics = evaluateSingle(aggregate).getRedisDistanceWithMetrics();

        assertEquals(1, metrics.getNumberOfEvaluatedKeys());
        assertEquals(H_MIN_VALUE, metrics.getDistance(), 1e-6);
    }

    @Test
    void testFtAggregateGroupByFieldMissingFromIndexedDocuments() {
        clientWith(Collections.singletonList("product:"), hashes("product:1", "redis handbook"));

        RedisCommand aggregate = new RedisCommand(RedisCommand.RedisCommandType.FT_AGGREGATE,
                new String[]{INDEX, "*", "GROUPBY", "1", "@category"}, true, 1);
        RedisDistanceWithMetrics metrics = evaluateSingle(aggregate).getRedisDistanceWithMetrics();

        assertTrue(metrics.getDistance() > H_MIN_VALUE);
    }

    @Test
    void testFtSearchWhenTheClientFailsReturnsMaxDistance() {
        ReflectionBasedRedisClient client = mock(ReflectionBasedRedisClient.class);
        when(client.getIndexPrefixes(anyString())).thenThrow(new RuntimeException("connection lost"));
        handler.setRedisClient(client);

        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("*")).getRedisDistanceWithMetrics();

        assertEquals(H_MAX_VALUE, metrics.getDistance(), 1e-6);
        assertEquals(0, metrics.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtSearchWithoutRedisClientReturnsMaxDistance() {
        RedisDistanceWithMetrics metrics = evaluateSingle(ftSearch("*")).getRedisDistanceWithMetrics();

        assertEquals(H_MAX_VALUE, metrics.getDistance(), 1e-6);
    }
}
