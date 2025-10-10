package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.internal.db.RedisDistanceWithMetrics;
import org.evomaster.client.java.controller.redis.RedisClient;
import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisHeuristicsCalculatorTest {

    private RedisHeuristicsCalculator calculator;
    private RedisClient client;

    @BeforeEach
    void setup() {
        calculator = new RedisHeuristicsCalculator();
        client = mock(RedisClient.class);
    }

    @Test
    void testKeysPatternExactMatch() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.KEYS,
                new String[]{"key<user*>"},
                true,
                5
        );

        when(client.getAllKeys()).thenReturn(new HashSet<>(Arrays.asList("user:1", "user:2", "other")));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, client);

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

        when(client.getAllKeys()).thenReturn(new HashSet<>(Arrays.asList("user:1", "user:2")));

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, client);
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

        when(client.getAllKeys()).thenReturn(new HashSet<>(Arrays.asList("user:1", "user:2")));

        RedisDistanceWithMetrics dClose = calculator.computeDistance(closeKey, client);
        RedisDistanceWithMetrics dFar = calculator.computeDistance(farKey, client);

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

        when(client.getAllKeys()).thenReturn(new HashSet<>(Arrays.asList("profile", "users")));
        when(client.getType("profile")).thenReturn("hash");
        when(client.getType("users")).thenReturn("hash");
        when(client.hashFieldExists("profile", "name")).thenReturn(true);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, client);

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

        when(client.getAllKeys()).thenReturn(new HashSet<>(Arrays.asList("profile")));
        when(client.getType("profile")).thenReturn("hash");
        when(client.hashFieldExists("profile", "age")).thenReturn(false);

        RedisDistanceWithMetrics result = calculator.computeDistance(cmd, client);

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

        when(client.getType("setA")).thenReturn("set");
        when(client.getType("setB")).thenReturn("set");
        when(client.getSetMembers("setA")).thenReturn(new HashSet<>(Arrays.asList("a", "b", "c")));
        when(client.getSetMembers("setB")).thenReturn(new HashSet<>(Arrays.asList("b", "c", "d")));

        RedisDistanceWithMetrics dIntersect = calculator.computeDistance(cmdIntersect, client);
        assertEquals(0.0, dIntersect.getDistance(),
                "Set intersection distance equals 0.0 when sets share members.");

        // No members in common
        RedisCommand cmdNoIntersect = new RedisCommand(
                RedisCommand.RedisCommandType.SINTER,
                new String[]{"key<setC>", "key<setD>"},
                true,
                1
        );

        when(client.getType("setC")).thenReturn("set");
        when(client.getType("setD")).thenReturn("set");
        when(client.getSetMembers("setC")).thenReturn(new HashSet<>(Arrays.asList("a", "b")));
        when(client.getSetMembers("setD")).thenReturn(new HashSet<>(Arrays.asList("c", "d")));

        RedisDistanceWithMetrics dNoIntersect = calculator.computeDistance(cmdNoIntersect, client);

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

        when(client.getType("setE")).thenReturn("set");
        when(client.getType("setF")).thenReturn("set");
        when(client.getSetMembers("setE")).thenReturn(new HashSet<>(Arrays.asList("a", "b")));
        when(client.getSetMembers("setF")).thenReturn(new HashSet<>(Arrays.asList("y", "z")));

        RedisDistanceWithMetrics dNoIntersectFarDistance = calculator.computeDistance(cmdNoIntersectFarDistance, client);

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

        when(client.getType("setA")).thenReturn("set");
        when(client.getType("setB")).thenReturn("set");
        when(client.getType("setC")).thenReturn("set");
        when(client.getType("setD")).thenReturn("set");
        when(client.getSetMembers("setA")).thenReturn(new HashSet<>(Arrays.asList("a", "b", "c")));
        when(client.getSetMembers("setB")).thenReturn(new HashSet<>(Arrays.asList("b", "c")));
        when(client.getSetMembers("setC")).thenReturn(new HashSet<>(Arrays.asList("c", "d")));
        when(client.getSetMembers("setD")).thenReturn(new HashSet<>(Arrays.asList("d", "e")));

        RedisDistanceWithMetrics dIntersectLessDistance = calculator.computeDistance(cmdIntersect, client);
        assertTrue(dIntersectLessDistance.getDistance() > 0.0,
                "With disjoint sets, distance must be greater than zero.");

        when(client.getType("setA")).thenReturn("set");
        when(client.getType("setB")).thenReturn("set");
        when(client.getType("setC")).thenReturn("set");
        when(client.getType("setD")).thenReturn("set");
        when(client.getSetMembers("setA")).thenReturn(new HashSet<>(Arrays.asList("a", "b", "c")));
        when(client.getSetMembers("setB")).thenReturn(new HashSet<>(Arrays.asList("b", "c")));
        when(client.getSetMembers("setC")).thenReturn(new HashSet<>(Arrays.asList("d", "e")));
        when(client.getSetMembers("setD")).thenReturn(new HashSet<>(Arrays.asList("f", "g")));

        RedisDistanceWithMetrics dIntersectMoreDistance = calculator.computeDistance(cmdIntersect, client);
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

        when(client.getKeysByType("set")).thenReturn(new HashSet<>(Arrays.asList("user:setA", "user:setB", "profile:set")));

        double dSimilar = calculator.computeDistance(similar, client).getDistance();
        double dDifferent = calculator.computeDistance(different, client).getDistance();

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

        when(client.getKeysByType("string")).thenReturn(new HashSet<>(Arrays.asList("session:1235", "config", "log")));

        double dSimilar = calculator.computeDistance(similar, client).getDistance();
        double dDifferent = calculator.computeDistance(different, client).getDistance();

        assertTrue(dSimilar < dDifferent,
                "GET with similar keys should yield smaller distance");
    }
}