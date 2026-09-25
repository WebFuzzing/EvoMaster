package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand;
import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFieldType;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
import org.evomaster.client.java.controller.redis.RedisIndexInfo;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.schemafields.GeoField;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TagField;
import redis.clients.jedis.search.schemafields.TextField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MIN_VALUE;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisHandlerIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private static final String PEOPLE_INDEX = "idx:people";
    private static final String MULTI_PREFIX_INDEX = "idx:multi";
    private static final String NO_PREFIX_INDEX = "idx:all";
    private static final String GEO_INDEX = "idx:geo";

    private GenericContainer<?> redisContainer;
    private ReflectionBasedRedisClient client;
    private UnifiedJedis jedis;
    private RedisHandler handler;
    private int port;

    @BeforeAll
    void setupContainer() {
        // redis-stack-server, not plain redis, so the RediSearch module is loaded server-side
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis/redis-stack-server:latest"))
                .withExposedPorts(REDIS_PORT);
        redisContainer.start();

        port = redisContainer.getMappedPort(REDIS_PORT);

        client = new ReflectionBasedRedisClient("localhost", port, 0);
        jedis = new UnifiedJedis(new HostAndPort("localhost", port));
    }

    /**
     * FLUSHALL on redis-stack also drops the index definitions, so the indexes are recreated
     * after every flush.
     */
    private void createIndexes() {
        jedis.ftCreate(PEOPLE_INDEX,
                FTCreateParams.createParams().prefix("person:"),
                Arrays.<SchemaField>asList(TextField.of("name"), NumericField.of("age"), TagField.of("street")));
        jedis.ftCreate(MULTI_PREFIX_INDEX,
                FTCreateParams.createParams().prefix("a:", "b:"),
                Collections.<SchemaField>singletonList(TextField.of("title")));
        jedis.ftCreate(NO_PREFIX_INDEX,
                FTCreateParams.createParams(),
                Collections.<SchemaField>singletonList(TextField.of("title")));
        jedis.ftCreate(GEO_INDEX,
                FTCreateParams.createParams().prefix("place:"),
                Arrays.<SchemaField>asList(TextField.of("name"), GeoField.of("location"), NumericField.of("rating")));
    }

    @BeforeEach
    void setupHandler() {
        handler = new RedisHandler();
        handler.setRedisClient(client);
        handler.setCalculateHeuristics(true);
        client.flushAll();
        createIndexes();
    }

    @AfterAll
    void teardown() {
        jedis.close();
        client.close();
        redisContainer.stop();
    }

    @Test
    void testHeuristicDistanceForStringExists() {
        client.setValue("user:1", "John");
        client.setValue("user:2", "Jane");
        assertEquals(2, client.getAllKeys().size());

        RedisCommand similarKeyCmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:3"},
                true,
                10
        );
        RedisCommand differentKeyCmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:82bd3bff-4567-40f4-a42e-27f87276199f"},
                true,
                10
        );

        handler.handle(similarKeyCmd);
        handler.handle(differentKeyCmd);

        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();
        assertEquals(2, evals.size(), "Should be two command evaluations.");

        RedisCommandEvaluation evalForSimilar = evals.get(0);
        assertNotNull(evalForSimilar.getRedisDistanceWithMetrics());
        RedisCommandEvaluation evalForDifferent = evals.get(1);
        assertNotNull(evalForDifferent.getRedisDistanceWithMetrics());

        double distanceForSimilar = evalForSimilar.getRedisDistanceWithMetrics().getDistance();
        int evaluatedForSimilar = evalForSimilar.getRedisDistanceWithMetrics().getNumberOfEvaluatedKeys();

        assertTrue(distanceForSimilar >= 0 && distanceForSimilar <= 1,
                "Distance should be between 0 and 1");
        assertEquals(2, evaluatedForSimilar, "Both keys should be evaluated.");

        double distanceForDifferent = evalForDifferent.getRedisDistanceWithMetrics().getDistance();
        int evaluatedForDifferent = evalForDifferent.getRedisDistanceWithMetrics().getNumberOfEvaluatedKeys();

        assertTrue(distanceForDifferent >= 0 && distanceForDifferent <= 1,
                "Distance should be between 0 and 1");
        assertEquals(2, evaluatedForDifferent, "Both keys should be evaluated.");
        assertTrue(distanceForSimilar < distanceForDifferent,
                "Distance for similar should be the smallest.");
    }

    @Test
    void testResetClearsCommands() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:1"},
                true,
                5
        );
        handler.handle(cmd);
        assertFalse(handler.getEvaluatedRedisCommands().isEmpty());
        handler.reset();
        assertTrue(handler.getEvaluatedRedisCommands().isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // FT.INFO based index resolution
    // ---------------------------------------------------------------------------------------

    @Test
    void testGetIndexInfoForSinglePrefixIndex() {
        RedisIndexInfo info = client.getIndexInfo(PEOPLE_INDEX);

        assertNotNull(info);
        assertTrue(info.isHashIndex());
        assertEquals(Collections.singletonList("person:"), info.getPrefixes());
        assertEquals(RedisSearchFieldType.TEXT, info.getAttributes().get("name"));
        assertEquals(RedisSearchFieldType.NUMERIC, info.getAttributes().get("age"));
        assertEquals(RedisSearchFieldType.TAG, info.getAttributes().get("street"));
    }

    @Test
    void testGetIndexInfoForMultiPrefixIndex() {
        RedisIndexInfo info = client.getIndexInfo(MULTI_PREFIX_INDEX);

        assertNotNull(info);
        assertEquals(new HashSet<>(Arrays.asList("a:", "b:")), new HashSet<>(info.getPrefixes()));
    }

    @Test
    void testGetIndexInfoForIndexWithoutPrefixCoversEverything() {
        RedisIndexInfo info = client.getIndexInfo(NO_PREFIX_INDEX);

        assertNotNull(info);
        assertEquals(Collections.singletonList(""), info.getPrefixes());
    }

    @Test
    void testGetIndexInfoKeepsTheOrderInWhichAttributesWereDeclared() {
        RedisIndexInfo info = client.getIndexInfo(PEOPLE_INDEX);

        assertEquals(Arrays.asList("name", "age", "street"), new ArrayList<>(info.getAttributes().keySet()));
    }

    @Test
    void testGetIndexInfoMapsTypesNotHandledByDataGenerationToOther() {
        RedisIndexInfo info = client.getIndexInfo(GEO_INDEX);

        assertEquals(RedisSearchFieldType.OTHER, info.getAttributes().get("location"),
                "GEO is a valid RediSearch type, but not one data generation handles");
        assertEquals(RedisSearchFieldType.TEXT, info.getAttributes().get("name"));
        assertEquals(RedisSearchFieldType.NUMERIC, info.getAttributes().get("rating"));
    }

    @Test
    void testGetIndexInfoForUnknownIndexIsNull() {
        assertNull(client.getIndexInfo("idx:does-not-exist"));
    }

    // ---------------------------------------------------------------------------------------
    // FT.SEARCH / FT.AGGREGATE heuristics against a real RediSearch index
    // ---------------------------------------------------------------------------------------

    private void seedPeople() {
        client.hashSet("person:1", "name", "alice");
        client.hashSet("person:1", "age", "30");
        client.hashSet("person:1", "street", "main");
        client.hashSet("person:2", "name", "bob");
        client.hashSet("person:2", "age", "45");
        client.hashSet("person:2", "street", "second");
        // matches name:alice but lives outside the index prefix, so Redis never indexes it
        client.hashSet("other:1", "name", "alice");
    }

    private RedisDistanceWithMetrics evaluate(RedisCommand.RedisCommandType type, String... args) {
        handler.handle(new RedisCommand(type, args, true, 1));
        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();
        assertEquals(1, evals.size());
        RedisDistanceWithMetrics metrics = evals.get(0).getRedisDistanceWithMetrics();
        handler.reset();
        return metrics;
    }

    private double searchDistance(String query) {
        return evaluate(RedisCommand.RedisCommandType.FT_SEARCH, PEOPLE_INDEX, query).getDistance();
    }

    @Test
    void testFtSearchHeuristicAgreesWithWhatRedisReturns() {
        seedPeople();

        String[] queries = {
                "*",
                "@name:alice", "@name:zzzz", "@name:ali*",
                "@age:[25 35]", "@age:[31 40]", "@age:[30 40]", "@age:[20 30]",
                "@street:{main}", "@street:{other|second}", "@street:{nothere}",
                "@name:alice @age:[25 35]", "@age:[25 35] @street:{second}",
                "@name:alice @age:[25 35] @street:{main}"
        };

        for (String query : queries) {
            long realMatches = jedis.ftSearch(PEOPLE_INDEX, query).getTotalResults();
            double distance = searchDistance(query);

            if (realMatches > 0) {
                assertEquals(H_MIN_VALUE, distance, 1e-6, "Redis matches '" + query + "', so distance must be 0");
            } else {
                assertTrue(distance > H_MIN_VALUE, "Redis returns nothing for '" + query + "', so distance must be > 0");
            }
        }
    }

    @Test
    void testFtSearchOnlyEvaluatesDocumentsCoveredByTheIndex() {
        seedPeople();

        RedisDistanceWithMetrics metrics = evaluate(RedisCommand.RedisCommandType.FT_SEARCH, PEOPLE_INDEX, "*");

        assertEquals(2, metrics.getNumberOfEvaluatedKeys(), "other:1 is a hash but is outside the index prefix");
    }

    @Test
    void testFtSearchIgnoresNonHashKeysUnderThePrefix() {
        seedPeople();
        client.setValue("person:string", "not a hash");

        RedisDistanceWithMetrics metrics = evaluate(RedisCommand.RedisCommandType.FT_SEARCH, PEOPLE_INDEX, "*");

        assertEquals(2, metrics.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtSearchCloserQueryHasSmallerDistance() {
        seedPeople();

        assertTrue(searchDistance("@age:[31 40]") < searchDistance("@age:[1000 2000]"));
        assertTrue(searchDistance("@street:{mains}") < searchDistance("@street:{zzzzzzzzzzzz}"));
    }

    @Test
    void testFtSearchDoesNotConsiderDocumentsOutsideTheIndexAsMatches() {
        seedPeople();
        client.hashSet("other:2", "name", "carol");

        // "carol" only exists in a non-indexed hash, exactly as Redis would see it
        assertEquals(0, jedis.ftSearch(PEOPLE_INDEX, "@name:carol").getTotalResults());
        assertTrue(searchDistance("@name:carol") > H_MIN_VALUE);
    }

    @Test
    void testFtSearchWithoutAnyDocumentsHasPositiveDistance() {
        RedisDistanceWithMetrics metrics = evaluate(RedisCommand.RedisCommandType.FT_SEARCH, PEOPLE_INDEX, "*");

        assertEquals(0, metrics.getNumberOfEvaluatedKeys());
        assertTrue(metrics.getDistance() > H_MIN_VALUE);
    }

    @Test
    void testFtSearchOnUnknownIndexHasPositiveDistance() {
        seedPeople();

        RedisDistanceWithMetrics metrics = evaluate(RedisCommand.RedisCommandType.FT_SEARCH, "idx:does-not-exist", "*");

        assertEquals(0, metrics.getNumberOfEvaluatedKeys());
        assertTrue(metrics.getDistance() > H_MIN_VALUE);
    }

    @Test
    void testFtSearchOnMultiPrefixAndPrefixlessIndexes() {
        client.hashSet("a:1", "title", "first");
        client.hashSet("b:1", "title", "second");
        client.hashSet("c:1", "title", "third");

        assertEquals(2, evaluate(RedisCommand.RedisCommandType.FT_SEARCH, MULTI_PREFIX_INDEX, "*").getNumberOfEvaluatedKeys());
        assertEquals(3, evaluate(RedisCommand.RedisCommandType.FT_SEARCH, NO_PREFIX_INDEX, "*").getNumberOfEvaluatedKeys());
        assertEquals(H_MIN_VALUE, evaluate(RedisCommand.RedisCommandType.FT_SEARCH, MULTI_PREFIX_INDEX, "@title:second").getDistance(), 1e-6);
        assertTrue(evaluate(RedisCommand.RedisCommandType.FT_SEARCH, MULTI_PREFIX_INDEX, "@title:third").getDistance() > H_MIN_VALUE,
                "c:1 is outside the a:/b: prefixes");
    }

    @Test
    void testFtAggregateGroupByExistingAndMissingField() {
        seedPeople();

        double existing = evaluate(RedisCommand.RedisCommandType.FT_AGGREGATE,
                PEOPLE_INDEX, "*", "GROUPBY", "1", "@street").getDistance();
        double missing = evaluate(RedisCommand.RedisCommandType.FT_AGGREGATE,
                PEOPLE_INDEX, "*", "GROUPBY", "1", "@country").getDistance();

        assertEquals(H_MIN_VALUE, existing, 1e-6);
        assertTrue(missing > existing);
    }

    @Test
    void testFtAggregateBaseQueryStillMatters() {
        seedPeople();

        double matching = evaluate(RedisCommand.RedisCommandType.FT_AGGREGATE,
                PEOPLE_INDEX, "@name:alice", "GROUPBY", "1", "@street").getDistance();
        double nonMatching = evaluate(RedisCommand.RedisCommandType.FT_AGGREGATE,
                PEOPLE_INDEX, "@name:zzzz", "GROUPBY", "1", "@street").getDistance();

        assertEquals(H_MIN_VALUE, matching, 1e-6);
        assertTrue(nonMatching > matching);
    }

    // ---------------------------------------------------------------------------------------
    // FT.SEARCH / FT.AGGREGATE: data generation - failed command registration against a real index
    // ---------------------------------------------------------------------------------------

    @Test
    void testFtSearchFailedCommandCarriesRealIndexSchema() {
        seedPeople();

        handler.handle(new RedisCommand(RedisCommand.RedisCommandType.FT_SEARCH,
                new String[]{PEOPLE_INDEX, "@name:zzzz"}, true, 1));
        handler.getEvaluatedRedisCommands();

        List<RedisFailedCommand> failedCommands = handler.getExecutionDto().failedCommands;
        assertEquals(1, failedCommands.size());

        RedisFailedCommand failed = failedCommands.get(0);
        assertEquals("FT_SEARCH", failed.command);
        assertEquals(PEOPLE_INDEX, failed.indexName);
        assertEquals(Collections.singletonList("person:"), failed.indexPrefixes);
        assertEquals(RedisSearchFieldType.TEXT, failed.indexAttributes.get("name"));
        assertEquals(RedisSearchFieldType.NUMERIC, failed.indexAttributes.get("age"));
        assertEquals(RedisSearchFieldType.TAG, failed.indexAttributes.get("street"));

        assertEquals(1, failed.filters.size());
        assertEquals(RedisSearchFieldType.TEXT, failed.filters.get(0).type);
        assertEquals("name", failed.filters.get(0).field);
        assertEquals("zzzz", failed.filters.get(0).term);
    }

    @Test
    void testFtSearchOnUnknownIndexDoesNotRegisterFailedCommand() {
        handler.handle(new RedisCommand(RedisCommand.RedisCommandType.FT_SEARCH,
                new String[]{"idx:does-not-exist", "*"}, true, 1));
        handler.getEvaluatedRedisCommands();

        assertTrue(handler.getExecutionDto().failedCommands.isEmpty());
    }

    @Test
    void testFtAggregateFailedCommandCarriesGroupByFields() {
        seedPeople();

        handler.handle(new RedisCommand(RedisCommand.RedisCommandType.FT_AGGREGATE,
                new String[]{PEOPLE_INDEX, "*", "GROUPBY", "1", "@country"}, true, 1));
        handler.getEvaluatedRedisCommands();

        List<RedisFailedCommand> failedCommands = handler.getExecutionDto().failedCommands;
        assertEquals(1, failedCommands.size());
        assertEquals("FT_AGGREGATE", failedCommands.get(0).command);
        assertEquals(Collections.singletonList("country"), failedCommands.get(0).groupByFields);
    }
}