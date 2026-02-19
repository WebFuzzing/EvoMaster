package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.controller.redis.RedisValueData;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

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
                new String[]{"key<user*>"},
                true,
                5
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:1", null);
        redisValueDataList.put("user:2", null);
        redisValueDataList.put("other", null);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisValueDataList);

        assertEquals(0.0, result.getDistance(), 1e-6, "Pattern 'user*' should fully match 'user:1' and 'user:2'");
    }

    @Test
    void testKeysPatternNoMatch() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.KEYS,
                new String[]{"key<thiskeydoesnotexist*>"},
                true,
                5
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:1", null);
        redisValueDataList.put("user:2", null);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisValueDataList);
        assertEquals(1.0, result.getDistance(), 0.1, "Pattern with no matches should yield max distance 1");
        assertEquals(2, result.getNumberOfEvaluatedKeys());
    }

    @Test
    void testExistsCommandSimilarity() {
        RedisCommand closeKey = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"key<user:3>"},
                true,
                5
        );

        RedisCommand farKey = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"key<abcxyz>"},
                true,
                5
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:1", null);
        redisValueDataList.put("user:2", null);

        RedisDistanceWithMetrics dClose = calculator.computeDistance(closeKey, redisValueDataList);
        RedisDistanceWithMetrics dFar = calculator.computeDistance(farKey, redisValueDataList);

        assertTrue(dClose.getDistance() < dFar.getDistance(),
                "Closer key should have smaller distance.");
    }

    @Test
    void testHGetFieldExists() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"key<profile>", "key<name>"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.singletonMap("name", "John")));
        redisValueDataList.put("users", new RedisValueData(Collections.emptyMap()));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisValueDataList);

        assertEquals(0.0, result.getDistance(), 1e-6,
                "Field 'name' exists, so distance must be 0");
        assertTrue(result.getNumberOfEvaluatedKeys() > 0);
    }

    @Test
    void testHGetFieldNotExists() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"key<profile>", "key<age>"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.emptyMap()));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisValueDataList);

        assertTrue(result.getDistance() > 0.0, "Missing field should yield positive distance");
        assertTrue(result.getNumberOfEvaluatedKeys() >= 1);
    }

    @Test
    void testHGetFieldDistance() {
        RedisCommand lowerDistanceCmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"key<profile>", "key<weight>"},
                true,
                3
        );
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"key<profile>", "key<age>"},
                true,
                3
        );
        RedisCommand greaterDistanceCmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                new String[]{"key<user>", "key<direction>"},
                true,
                3
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("profile", new RedisValueData(Collections.singletonMap("height", "175")));

        RedisDistanceWithMetrics resultLower = calculator.computeDistance(lowerDistanceCmd, redisValueDataList);
        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisValueDataList);
        RedisDistanceWithMetrics resultGreater = calculator.computeDistance(greaterDistanceCmd, redisValueDataList);

        assertTrue(resultLower.getDistance() < result.getDistance(),
                "Closer target field should yield lower distance");
        assertTrue(result.getDistance() < resultGreater.getDistance(),
                "Closer target key and field should yield lower distance");
    }

    /**
     * Distance in intersection between two given sets:
     * - setA y setB share members → low distance
     * - setC y setD have no members in common (but close) → greater distance
     * - setE y setF have no members in common (very different) → the greatest distance
     */
    @Test
    void testSInterSetsIntersectionAndNoIntersection() {
        // Shared members.
        RedisCommand cmdIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"key<setA>", "key<setB>"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListIntersection = new HashMap<>();
        redisValueDataListIntersection.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataListIntersection.put("setB", new RedisValueData(new HashSet<>(Arrays.asList("b", "c", "d"))));

        RedisDistanceWithMetrics dIntersect = calculator.computeDistance(cmdIntersect, redisValueDataListIntersection);
        assertEquals(0.0, dIntersect.getDistance(),
                "Set intersection distance equals 0.0 when sets share members.");

        // No members in common
        RedisCommand cmdNoIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"key<setC>", "key<setD>"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListNoIntersection = new HashMap<>();
        redisValueDataListNoIntersection.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("a", "b"))));
        redisValueDataListNoIntersection.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("c", "d"))));

        RedisDistanceWithMetrics dNoIntersect = calculator.computeDistance(cmdNoIntersect, redisValueDataListNoIntersection);

        assertTrue(dNoIntersect.getDistance() > 0.0,
                "With disjoint sets, distance must be greater than zero.");
        assertTrue(dIntersect.getDistance() < dNoIntersect.getDistance(),
                "Sets with common elements should yield smaller distance");

        // No members in common, greater distance
        RedisCommand cmdNoIntersectFarDistance = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"key<setE>", "key<setF>"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListGreaterDisjoint = new HashMap<>();
        redisValueDataListGreaterDisjoint.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("a", "b"))));
        redisValueDataListGreaterDisjoint.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("y", "z"))));

        RedisDistanceWithMetrics dNoIntersectFarDistance = calculator.computeDistance(cmdNoIntersectFarDistance, redisValueDataListGreaterDisjoint);

        assertTrue(dNoIntersectFarDistance.getDistance() > 0.0,
                "With disjoint sets, distance must be greater than zero.");
        assertTrue(dNoIntersect.getDistance() < dNoIntersectFarDistance.getDistance(),
                "Sets with close elements should yield smaller distance");
    }

    /**
     * Distance in intersection between several sets.
     * When there's no intersection between all of them, distance should be greater as fewer intersections are possible.
     */
    @Test
    void testSInterSeveralSets() {
        RedisCommand cmdIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"key<setA>", "key<setB>", "key<setC>", "key<setD>"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataListLessDistance = new HashMap<>();
        redisValueDataListLessDistance.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataListLessDistance.put("setB", new RedisValueData(new HashSet<>(Arrays.asList("b", "c"))));
        redisValueDataListLessDistance.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("c", "d"))));
        redisValueDataListLessDistance.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("d", "e"))));

        RedisDistanceWithMetrics dIntersectLessDistance = calculator.computeDistance(cmdIntersect, redisValueDataListLessDistance);
        assertTrue(dIntersectLessDistance.getDistance() > 0.0,
                "With disjoint sets, distance must be greater than zero.");

        Map<String, RedisValueData> redisValueDataListMoreDistance = new HashMap<>();
        redisValueDataListMoreDistance.put("setA", new RedisValueData(new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisValueDataListMoreDistance.put("setB", new RedisValueData(new HashSet<>(Arrays.asList("b", "c"))));
        redisValueDataListMoreDistance.put("setC", new RedisValueData(new HashSet<>(Arrays.asList("d", "e"))));
        redisValueDataListMoreDistance.put("setD", new RedisValueData(new HashSet<>(Arrays.asList("f", "g"))));

        RedisDistanceWithMetrics dIntersectMoreDistance = calculator.computeDistance(cmdIntersect, redisValueDataListMoreDistance);
        assertTrue(dIntersectMoreDistance.getDistance() > 0.0,
                "With disjoint sets, distance must be greater than zero.");

        assertTrue(dIntersectMoreDistance.getDistance() > dIntersectLessDistance.getDistance(),
                "Distance should be greater as fewer intersections are possible.");
    }

    @Test
    void testSMembersSimilarity() {
        RedisCommand similar = new RedisCommand(
                RedisCommand.RedisCommandType.SMEMBERS,
                new String[]{"key<user:set1>"},
                true,
                2
        );
        RedisCommand different = new RedisCommand(
                RedisCommand.RedisCommandType.SMEMBERS,
                new String[]{"key<orders>"},
                true,
                2
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("user:setA", null);
        redisValueDataList.put("user:setB", null);
        redisValueDataList.put("profile:set", null);

        double dSimilar = calculator.computeDistance(similar, redisValueDataList).getDistance();
        double dDifferent = calculator.computeDistance(different, redisValueDataList).getDistance();

        assertTrue(dSimilar < dDifferent,
                "SMEMBERS with similar keys should yield smaller distance");
    }

    @Test
    void testGetCommandSimilarity() {
        RedisCommand similar = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                new String[]{"key<session:1234>"},
                true,
                1
        );

        RedisCommand different = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                new String[]{"key<orders>"},
                true,
                1
        );

        Map<String, RedisValueData> redisValueDataList = new HashMap<>();
        redisValueDataList.put("session:1235", null);
        redisValueDataList.put("config", null);
        redisValueDataList.put("log", null);

        double dSimilar = calculator.computeDistance(similar, redisValueDataList).getDistance();
        double dDifferent = calculator.computeDistance(different, redisValueDataList).getDistance();

        assertTrue(dSimilar < dDifferent,
                "GET with similar keys should yield smaller distance");
    }
}