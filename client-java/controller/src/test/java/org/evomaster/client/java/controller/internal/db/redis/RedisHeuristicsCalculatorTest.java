package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.controller.redis.RedisKeyValueStore;
import org.evomaster.client.java.controller.redis.RedisValueData;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MAX_VALUE;
import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MIN_VALUE;
import static org.junit.jupiter.api.Assertions.*;

class RedisHeuristicsCalculatorTest {

    private RedisHeuristicsCalculator calculator;

    @BeforeEach
    void setup() {
        calculator = new RedisHeuristicsCalculator();
    }

    @Test
    void testKeysPatternExactMatch() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.KEYS,
                new String[]{"user*"},
                true,
                5
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:1", null);
        redisValueDataList.put("user:2", null);
        redisValueDataList.put("other", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertEquals(H_MIN_VALUE, result.getDistance(), 1e-6, "Pattern 'user*' should fully match 'user:1' and 'user:2'");
    }

    @Test
    void testKeysPatternNoMatch() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.KEYS,
                new String[]{"thiskeydoesnotexist*"},
                true,
                5
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:1", null);
        redisValueDataList.put("user:2", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);
        assertEquals(1.0, result.getDistance(), 0.25, "Pattern with no matches should yield values close to 1");
        assertEquals(2, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testExistsCommandSimilarity() {
        RedisCommand closeKey = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:3"},
                true,
                5
        );

        RedisCommand farKey = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"abcxyz"},
                true,
                5
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:1", null);
        redisValueDataList.put("user:2", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics dClose = calculator.computeDistance(closeKey, redisKeyValueStore);
        RedisDistanceWithMetrics dFar = calculator.computeDistance(farKey, redisKeyValueStore);

        assertTrue(dClose.getDistance() < dFar.getDistance(),
                "Closer key should have smaller distance.");
    }

    @Test
    void testHGetFieldExists() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"profile", "name"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.singletonMap("name", "John")));
        redisValueDataList.put("users", new RedisValueData(Collections.emptyMap()));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertEquals(H_MIN_VALUE, result.getDistance(), 1e-6,
                "Field 'name' exists, so distance must be 0");
        assertTrue(result.getNumberOfEvaluatedKeys() > 0);
    }

    @Test
    void testHGetFieldNotExists() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"profile", "age"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.emptyMap()));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertTrue(result.getDistance() > H_MIN_VALUE, "Missing field should yield positive distance");
        assertTrue(result.getNumberOfEvaluatedKeys() >= 1);
    }

    @Test
    void testHGetFieldDistance() {
        RedisCommand lowerDistanceCmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"profile", "weight"},
                true,
                3
        );
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"profile", "age"},
                true,
                3
        );
        RedisCommand greaterDistanceCmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"user", "direction"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.singletonMap("height", "175")));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics resultLower = calculator.computeDistance(lowerDistanceCmd, redisKeyValueStore);
        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);
        RedisDistanceWithMetrics resultGreater = calculator.computeDistance(greaterDistanceCmd, redisKeyValueStore);

        assertTrue(resultLower.getDistance() < result.getDistance(),
                "Closer target field should yield lower distance");
        assertTrue(result.getDistance() < resultGreater.getDistance(),
                "Closer target key and field should yield lower distance");
    }

    @Test
    void testSInterSetsIntersectionAndNoIntersection() {
        RedisCommand cmdIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setA", "setB"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListIntersection = new HashMap<>();
        redisValueDataListIntersection.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataListIntersection.put("setB", new RedisValueData(new HashSet<>(Arrays.asList("b", "c", "d"))));
        RedisKeyValueStore redisKeyValueStoreIntersection = new RedisKeyValueStore(redisValueDataListIntersection);

        RedisDistanceWithMetrics dIntersect = calculator.computeDistance(cmdIntersect, redisKeyValueStoreIntersection);
        assertEquals(H_MIN_VALUE, dIntersect.getDistance(),
                "Set intersection distance equals H_MIN_VALUE when sets share members.");

        RedisCommand cmdNoIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setC", "setD"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListNoIntersection = new HashMap<>();
        redisValueDataListNoIntersection.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("a", "b"))));
        redisValueDataListNoIntersection.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("c", "d"))));
        RedisKeyValueStore redisKeyValueStoreNoIntersection = new RedisKeyValueStore(redisValueDataListNoIntersection);

        RedisDistanceWithMetrics dNoIntersect = calculator.computeDistance(cmdNoIntersect, redisKeyValueStoreNoIntersection);

        assertTrue(dNoIntersect.getDistance() > H_MIN_VALUE,
                "With disjoint sets, distance must be greater than zero.");
        assertTrue(dIntersect.getDistance() < dNoIntersect.getDistance(),
                "Sets with common elements should yield smaller distance");

        RedisCommand cmdNoIntersectFarDistance = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setE", "setF"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListGreaterDisjoint = new HashMap<>();
        redisValueDataListGreaterDisjoint.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("a", "b"))));
        redisValueDataListGreaterDisjoint.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("y", "z"))));
        RedisKeyValueStore redisKeyValueStoreGreaterDisjoint = new RedisKeyValueStore(redisValueDataListGreaterDisjoint);

        RedisDistanceWithMetrics dNoIntersectFarDistance = calculator.computeDistance(cmdNoIntersectFarDistance, redisKeyValueStoreGreaterDisjoint);

        assertTrue(dNoIntersectFarDistance.getDistance() > H_MIN_VALUE,
                "With disjoint sets, distance must be greater than zero.");
        assertTrue(dNoIntersect.getDistance() < dNoIntersectFarDistance.getDistance(),
                "Sets with close elements should yield smaller distance");
    }

    @Test
    void testSInterSeveralSets() {
        RedisCommand cmdIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setA", "setB", "setC", "setD"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListLessDistance = new HashMap<>();
        redisValueDataListLessDistance.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataListLessDistance.put("setB", new RedisValueData(new HashSet<>(Arrays.asList("b", "c"))));
        redisValueDataListLessDistance.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("c", "d"))));
        redisValueDataListLessDistance.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("d", "e"))));
        RedisKeyValueStore redisKeyValueStoreLessDistance = new RedisKeyValueStore(redisValueDataListLessDistance);

        RedisDistanceWithMetrics dIntersectLessDistance = calculator.computeDistance(cmdIntersect, redisKeyValueStoreLessDistance);
        assertTrue(dIntersectLessDistance.getDistance() > H_MIN_VALUE,
                "With disjoint sets, distance must be greater than zero.");

        Map<String, RedisValueData> redisValueDataListMoreDistance = new HashMap<>();
        redisValueDataListMoreDistance.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataListMoreDistance.put("setB", new RedisValueData(new HashSet<>(Arrays.asList("b", "c"))));
        redisValueDataListMoreDistance.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("d", "e"))));
        redisValueDataListMoreDistance.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("f", "g"))));
        RedisKeyValueStore redisKeyValueStoreMoreDistance = new RedisKeyValueStore(redisValueDataListMoreDistance);

        RedisDistanceWithMetrics dIntersectMoreDistance = calculator.computeDistance(cmdIntersect, redisKeyValueStoreMoreDistance);
        assertTrue(dIntersectMoreDistance.getDistance() > H_MIN_VALUE,
                "With disjoint sets, distance must be greater than zero.");

        assertTrue(dIntersectMoreDistance.getDistance() > dIntersectLessDistance.getDistance(),
                "Distance should be greater as fewer intersections are possible.");
    }

    @Test
    void testSMembersSimilarity() {
        RedisCommand similar = new RedisCommand(
                RedisCommand.RedisCommandType.SMEMBERS,
                new String[]{"user:set1"},
                true,
                2
        );
        RedisCommand different = new RedisCommand(
                RedisCommand.RedisCommandType.SMEMBERS,
                new String[]{"orders"},
                true,
                2
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:setA", null);
        redisValueDataList.put("user:setB", null);
        redisValueDataList.put("profile:set", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        double dSimilar = calculator.computeDistance(similar, redisKeyValueStore).getDistance();
        double dDifferent = calculator.computeDistance(different, redisKeyValueStore).getDistance();

        assertTrue(dSimilar < dDifferent,
                "SMEMBERS with similar keys should yield smaller distance");
    }

    @Test
    void testGetCommandSimilarity() {
        RedisCommand similar = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                new String[]{"session:1234"},
                true,
                1
        );

        RedisCommand different = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                new String[]{"orders"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("session:1235", null);
        redisValueDataList.put("config", null);
        redisValueDataList.put("log", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        double dSimilar = calculator.computeDistance(similar, redisKeyValueStore).getDistance();
        double dDifferent = calculator.computeDistance(different, redisKeyValueStore).getDistance();

        assertTrue(dSimilar < dDifferent,
                "GET with similar keys should yield smaller distance");
    }

    @Test
    void testComputeDistanceHandlesInternalExceptionOk() {
        RedisCommand malformedHGet = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"profile"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.singletonMap("name", "John")));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(malformedHGet, redisKeyValueStore);

        assertEquals(H_MAX_VALUE, result.getDistance(), 1e-6,
                "An internal exception must translate into maximum distance");
        assertEquals(0, result.getNumberOfEvaluatedKeys(),
                "An internal exception must not report evaluated keys");
    }

    @Test
    void testUnsupportedCommandTypeReturnsMaxDistance() {
        RedisCommand unsupported = new RedisCommand(
                RedisCommand.RedisCommandType.SET,
                new String[]{"foo", "bar"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("foo", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(unsupported, redisKeyValueStore);

        assertEquals(H_MAX_VALUE, result.getDistance(), 1e-6,
                "An unsupported command type must return maximum distance");
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testHGetAllCommand() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGETALL,
                new String[]{"profile"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertEquals(H_MIN_VALUE, result.getDistance(), 1e-6, "HGETALL on an existing key must yield distance 0");
    }

    @Test
    void testKeyMatchAgainstEmptyDatabase() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                new String[]{"anykey"},
                true,
                1
        );

        RedisKeyValueStore emptyStore = new RedisKeyValueStore(new HashMap<>());

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, emptyStore);

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "An empty database can never yield a perfect match");
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testKeysInvalidPatternIsHandledOk() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.KEYS,
                new String[]{"[abc"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("abc", null);
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = assertDoesNotThrow(
                () -> calculator.computeDistance(cmd, redisKeyValueStore),
                "An invalid pattern must not propagate an exception"
        );

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "An invalid pattern can never translate into a perfect match");
    }

    @Test
    void testKeysAgainstEmptyDatabase() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.KEYS,
                new String[]{"user*"},
                true,
                1
        );

        RedisKeyValueStore emptyStore = new RedisKeyValueStore(new HashMap<>());

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, emptyStore);

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "An empty database can never yield a perfect match for KEYS");
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testSInterWithNoKeysReturnsMaxDistance() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b"))));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "SINTER with no keys can never yield a perfect match");
    }

    @Test
    void testSInterAgainstEmptyDatabase() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setA", "setB"},
                true,
                1
        );

        RedisKeyValueStore emptyStore = new RedisKeyValueStore(new HashMap<>());

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, emptyStore);

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "SINTER against an empty database can never yield a perfect match");
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testSInterWithMissingSetKeyReturnsMaxDistance() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setA", "setB"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b"))));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "If a referenced set does not exist, the intersection can never be a perfect match");
    }

    @Test
    void testSInterWithAllEmptySetsReturnsMaxDistance() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setA", "setB"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("setA", new RedisValueData(new HashSet<>()));
        redisValueDataList.put("setB", new RedisValueData(new HashSet<>()));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisKeyValueStore);

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "Empty (non-null) sets cannot yield a perfect match in an intersection");
    }

    @Test
    void testSInterOneEmptySetAmongNonEmptySetsDoesNotThrow() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"setA", "setB"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataList.put("setB", new RedisValueData(new HashSet<>()));
        RedisKeyValueStore redisKeyValueStore = new RedisKeyValueStore(redisValueDataList);

        RedisDistanceWithMetrics result = assertDoesNotThrow(
                () -> calculator.computeDistance(cmd, redisKeyValueStore),
                "An empty set of members in hContains must not throw an exception"
        );

        assertTrue(result.getDistance() > H_MIN_VALUE,
                "There can be no real intersection with an empty set");
    }

    // ---------------------------------------------------------------------------------------
    // FT.SEARCH / FT.AGGREGATE
    //
    // RedisHandler pre-filters the store down to the candidate documents of the index, so every
    // entry below plays the role of a HASH document covered by the index being queried.
    // ---------------------------------------------------------------------------------------

    private static RedisCommand ftSearch(String query) {
        return new RedisCommand(RedisCommand.RedisCommandType.FT_SEARCH,
                new String[]{"idx:people", query}, true, 1);
    }

    private static RedisCommand ftAggregate(String query, String... pipeline) {
        List<String> args = new ArrayList<>(Arrays.asList("idx:people", query));
        args.addAll(Arrays.asList(pipeline));
        return new RedisCommand(RedisCommand.RedisCommandType.FT_AGGREGATE,
                args.toArray(new String[0]), true, 1);
    }

    private static Map<String, String> person(String name, String age, String street) {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("age", age);
        fields.put("street", street);
        return fields;
    }

    private static RedisKeyValueStore peopleStore() {
        Map<String, RedisValueData> data = new HashMap<>();
        data.put("person:1", new RedisValueData(person("alice", "30", "main")));
        data.put("person:2", new RedisValueData(person("bob", "45", "second")));
        return new RedisKeyValueStore(data);
    }

    private double distance(RedisCommand cmd) {
        return calculator.computeDistance(cmd, peopleStore()).getDistance();
    }

    @Test
    void testFtSearchMatchAllQueryOverExistingCandidates() {
        RedisDistanceWithMetrics result = calculator.computeDistance(ftSearch("*"), peopleStore());

        assertEquals(H_MIN_VALUE, result.getDistance(), 1e-6, "'*' matches every candidate document");
        assertEquals(2, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtSearchWithNoCandidatesYieldsPositiveDistance() {
        RedisKeyValueStore emptyStore = new RedisKeyValueStore(new HashMap<>());

        RedisDistanceWithMetrics result = calculator.computeDistance(ftSearch("*"), emptyStore);

        assertTrue(result.getDistance() > H_MIN_VALUE, "An index without candidates can never match");
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtSearchTagFilterMatch() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("@street:{main}")), 1e-6);
        assertEquals(H_MIN_VALUE, distance(ftSearch("@street:{other|second}")), 1e-6,
                "Any value of the tag list matching is enough");
    }

    @Test
    void testFtSearchTagFilterCloserValueHasSmallerDistance() {
        double close = distance(ftSearch("@street:{mains}"));
        double far = distance(ftSearch("@street:{zzzzzzzzzzzz}"));

        assertTrue(close > H_MIN_VALUE);
        assertTrue(close < far, "A tag value closer to an existing one should be nearer");
    }

    @Test
    void testFtSearchTagFilterOnMissingFieldIsWorseThanExistingField() {
        double missingField = distance(ftSearch("@country:{main}"));
        double wrongValue = distance(ftSearch("@street:{mains}"));

        assertTrue(missingField > H_MIN_VALUE);
        assertTrue(wrongValue < missingField, "A field that does not exist anywhere is the farthest option");
    }

    @Test
    void testFtSearchNumericFilterInRange() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("@age:[25 35]")), 1e-6);
    }

    @Test
    void testFtSearchNumericFilterBoundsAreInclusive() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("@age:[30 40]")), 1e-6, "Lower bound is inclusive");
        assertEquals(H_MIN_VALUE, distance(ftSearch("@age:[20 30]")), 1e-6, "Upper bound is inclusive");
    }

    @Test
    void testFtSearchNumericFilterCloserRangeHasSmallerDistance() {
        double close = distance(ftSearch("@age:[31 40]")); // nearest ages are 30 and 45 -> 1 away
        double far = distance(ftSearch("@age:[100 200]"));

        assertTrue(close > H_MIN_VALUE);
        assertTrue(close < far, "A range closer to an existing value should be nearer");
    }

    @Test
    void testFtSearchNumericFilterOnNonNumericFieldNeverMatches() {
        assertTrue(distance(ftSearch("@name:[1 10]")) > H_MIN_VALUE);
    }

    @Test
    void testFtSearchTextFilterOnField() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("@name:alice")), 1e-6);
        assertEquals(H_MIN_VALUE, distance(ftSearch("@name:ali*")), 1e-6, "Prefix term matches");
    }

    @Test
    void testFtSearchTextTermRegexMetacharactersAreTakenLiterally() {
        assertTrue(distance(ftSearch("@name:a.ice")) > H_MIN_VALUE,
                "'.' in the term is not a wildcard, so 'a.ice' does not match 'alice'");
        assertTrue(distance(ftSearch("@name:al*ce")) > H_MIN_VALUE,
                "Only a trailing '*' is a prefix marker");

        double unbalanced = distance(ftSearch("@name:(alice"));
        assertTrue(unbalanced > H_MIN_VALUE);
        assertTrue(unbalanced < H_MAX_VALUE,
                "A term with regex metacharacters must still be compared, not fail with an invalid regex");
    }

    @Test
    void testFtSearchTextFilterWithoutFieldSearchesAllFields() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("second")), 1e-6,
                "A bare term may match any field of a document");
        assertTrue(distance(ftSearch("nonexistentterm")) > H_MIN_VALUE);
    }

    @Test
    void testFtSearchTextFilterOnMissingFieldIsWorseThanExistingField() {
        double missingField = distance(ftSearch("@country:alice"));
        double wrongTerm = distance(ftSearch("@name:zzzz"));

        assertTrue(missingField > H_MIN_VALUE);
        assertTrue(wrongTerm < missingField);
    }

    @Test
    void testFtSearchAllFiltersMustHoldOnTheSameDocument() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("@name:alice @age:[25 35] @street:{main}")), 1e-6);

        // alice is 30 and lives in "main", bob is 45 and lives in "second": no document has both
        double crossed = distance(ftSearch("@age:[25 35] @street:{second}"));
        assertTrue(crossed > H_MIN_VALUE, "Filters satisfied by different documents must not add up to a match");
    }

    @Test
    void testFtSearchMoreSatisfiedFiltersMeansSmallerDistance() {
        double oneWrong = distance(ftSearch("@name:alice @age:[100 200]"));
        double twoWrong = distance(ftSearch("@name:zzzz @age:[100 200]"));

        assertTrue(oneWrong > H_MIN_VALUE);
        assertTrue(oneWrong < twoWrong);
    }

    @Test
    void testFtSearchNumericFilterKeepsRangeSpaceInsideBrackets() {
        assertEquals(H_MIN_VALUE, distance(ftSearch("  @name:alice    @age:[ 25   35 ]  ")), 1e-6,
                "Extra whitespace around and inside the filters must be tolerated");
    }

    @Test
    void testFtSearchMalformedQueryDoesNotThrowAndNeverMatches() {
        for (String malformed : new String[]{"@age:[25 35", "@street:{main", "@age:[25]", "@age:[a b]"}) {
            RedisDistanceWithMetrics result = assertDoesNotThrow(
                    () -> calculator.computeDistance(ftSearch(malformed), peopleStore()),
                    "Malformed query must not propagate an exception: " + malformed);
            assertTrue(result.getDistance() > H_MIN_VALUE, "Malformed query can't be a perfect match: " + malformed);
        }
    }

    @Test
    void testFtSearchWithMissingQueryArgumentReturnsMaxDistance() {
        RedisCommand malformed = new RedisCommand(RedisCommand.RedisCommandType.FT_SEARCH,
                new String[]{"idx:people"}, true, 1);

        RedisDistanceWithMetrics result = calculator.computeDistance(malformed, peopleStore());

        assertEquals(H_MAX_VALUE, result.getDistance(), 1e-6);
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtAggregateGroupByExistingField() {
        RedisDistanceWithMetrics result = calculator.computeDistance(
                ftAggregate("*", "GROUPBY", "1", "@street"), peopleStore());

        assertEquals(H_MIN_VALUE, result.getDistance(), 1e-6);
        assertEquals(2, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testFtAggregateGroupByMissingFieldIsWorseThanExistingField() {
        double existing = distance(ftAggregate("*", "GROUPBY", "1", "@street"));
        double missing = distance(ftAggregate("*", "GROUPBY", "1", "@country"));

        assertTrue(missing > existing);
    }

    @Test
    void testFtAggregateAllGroupByFieldsMustExist() {
        double allExist = distance(ftAggregate("*", "GROUPBY", "2", "@street", "@name"));
        double oneMissing = distance(ftAggregate("*", "GROUPBY", "2", "@street", "@country"));

        assertEquals(H_MIN_VALUE, allExist, 1e-6);
        assertTrue(oneMissing > allExist);
    }

    @Test
    void testFtAggregateWithoutGroupByBehavesAsSearch() {
        assertEquals(distance(ftSearch("@name:alice")),
                distance(ftAggregate("@name:alice", "SORTBY", "2", "@age", "ASC")), 1e-9,
                "Stages other than GROUPBY are heuristically transparent");
        assertEquals(distance(ftSearch("@name:zzzz")),
                distance(ftAggregate("@name:zzzz")), 1e-9);
    }

    @Test
    void testFtAggregateIgnoresStagesAfterGroupByFields() {
        double withReduce = distance(ftAggregate("*", "GROUPBY", "1", "@street",
                "REDUCE", "COUNT", "0", "AS", "n"));

        assertEquals(H_MIN_VALUE, withReduce, 1e-6, "REDUCE args must not be mistaken for GROUPBY fields");
    }

    @Test
    void testFtAggregateBaseQueryStillMatters() {
        double matchingQuery = distance(ftAggregate("@name:alice", "GROUPBY", "1", "@street"));
        double nonMatchingQuery = distance(ftAggregate("@name:zzzz", "GROUPBY", "1", "@street"));

        assertEquals(H_MIN_VALUE, matchingQuery, 1e-6);
        assertTrue(nonMatchingQuery > matchingQuery);
    }

    @Test
    void testFtAggregateWithNoCandidatesYieldsPositiveDistance() {
        RedisDistanceWithMetrics result = calculator.computeDistance(
                ftAggregate("*", "GROUPBY", "1", "@street"),
                new RedisKeyValueStore(new HashMap<>()));

        assertTrue(result.getDistance() > H_MIN_VALUE);
        assertEquals(0, result.getNumberOfEvaluatedKeys());
    }
}