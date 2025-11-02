package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.controller.redis.RedisInfo;
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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("user:1"));
        redisInfoList.add(new RedisInfo("user:2"));
        redisInfoList.add(new RedisInfo("other"));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisInfoList);

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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("user:1"));
        redisInfoList.add(new RedisInfo("user:2"));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisInfoList);
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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("user:1"));
        redisInfoList.add(new RedisInfo("user:2"));

        RedisDistanceWithMetrics dClose = calculator.computeDistance(closeKey, redisInfoList);
        RedisDistanceWithMetrics dFar = calculator.computeDistance(farKey, redisInfoList);

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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("profile", true));
        redisInfoList.add(new RedisInfo("users", false));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisInfoList);

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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("profile", false));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, redisInfoList);

        assertTrue(result.getDistance() > 0.0, "Missing field should yield positive distance");
        assertTrue(result.getNumberOfEvaluatedKeys() >= 1);
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

        List<RedisInfo> redisInfoListIntersection = new ArrayList<>();
        redisInfoListIntersection.add(new RedisInfo("setA", "set", new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisInfoListIntersection.add(new RedisInfo("setB", "set", new HashSet<>(Arrays.asList("b", "c", "d"))));

        RedisDistanceWithMetrics dIntersect = calculator.computeDistance(cmdIntersect, redisInfoListIntersection);
        assertEquals(0.0, dIntersect.getDistance(),
                "Set intersection distance equals 0.0 when sets share members.");

        // No members in common
        RedisCommand cmdNoIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"key<setC>", "key<setD>"},
                true,
                1
        );

        List<RedisInfo> redisInfoListNoIntersection = new ArrayList<>();
        redisInfoListNoIntersection.add(new RedisInfo("setC", "set", new HashSet<>(Arrays.asList("a", "b"))));
        redisInfoListNoIntersection.add(new RedisInfo("setD", "set", new HashSet<>(Arrays.asList("c", "d"))));

        RedisDistanceWithMetrics dNoIntersect = calculator.computeDistance(cmdNoIntersect, redisInfoListNoIntersection);

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

        List<RedisInfo> redisInfoListGreaterDisjoint = new ArrayList<>();
        redisInfoListGreaterDisjoint.add(new RedisInfo("setC", "set", new HashSet<>(Arrays.asList("a", "b"))));
        redisInfoListGreaterDisjoint.add(new RedisInfo("setD", "set", new HashSet<>(Arrays.asList("y", "z"))));

        RedisDistanceWithMetrics dNoIntersectFarDistance = calculator.computeDistance(cmdNoIntersectFarDistance, redisInfoListGreaterDisjoint);

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

        List<RedisInfo> redisInfoListLessDistance = new ArrayList<>();
        redisInfoListLessDistance.add(new RedisInfo("setA", "set", new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisInfoListLessDistance.add(new RedisInfo("setB", "set", new HashSet<>(Arrays.asList("b", "c"))));
        redisInfoListLessDistance.add(new RedisInfo("setC", "set", new HashSet<>(Arrays.asList("c", "d"))));
        redisInfoListLessDistance.add(new RedisInfo("setD", "set", new HashSet<>(Arrays.asList("d", "e"))));

        RedisDistanceWithMetrics dIntersectLessDistance = calculator.computeDistance(cmdIntersect, redisInfoListLessDistance);
        assertTrue(dIntersectLessDistance.getDistance() > 0.0,
                "With disjoint sets, distance must be greater than zero.");

        List<RedisInfo> redisInfoListMoreDistance = new ArrayList<>();
        redisInfoListMoreDistance.add(new RedisInfo("setA", "set", new HashSet<>(Arrays.asList("a", "b", "c"))));
        redisInfoListMoreDistance.add(new RedisInfo("setB", "set", new HashSet<>(Arrays.asList("b", "c"))));
        redisInfoListMoreDistance.add(new RedisInfo("setC", "set", new HashSet<>(Arrays.asList("d", "e"))));
        redisInfoListMoreDistance.add(new RedisInfo("setD", "set", new HashSet<>(Arrays.asList("f", "g"))));

        RedisDistanceWithMetrics dIntersectMoreDistance = calculator.computeDistance(cmdIntersect, redisInfoListMoreDistance);
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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("user:setA"));
        redisInfoList.add(new RedisInfo("user:setB"));
        redisInfoList.add(new RedisInfo("profile:set"));

        double dSimilar = calculator.computeDistance(similar, redisInfoList).getDistance();
        double dDifferent = calculator.computeDistance(different, redisInfoList).getDistance();

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

        List<RedisInfo> redisInfoList = new ArrayList<>();
        redisInfoList.add(new RedisInfo("session:1235"));
        redisInfoList.add(new RedisInfo("config"));
        redisInfoList.add(new RedisInfo("log"));

        double dSimilar = calculator.computeDistance(similar, redisInfoList).getDistance();
        double dDifferent = calculator.computeDistance(different, redisInfoList).getDistance();

        assertTrue(dSimilar < dDifferent,
                "GET with similar keys should yield smaller distance");
    }
}