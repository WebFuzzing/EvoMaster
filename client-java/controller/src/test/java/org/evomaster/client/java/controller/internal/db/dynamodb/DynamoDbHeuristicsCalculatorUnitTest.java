package org.evomaster.client.java.controller.internal.db.dynamodb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.evomaster.client.java.controller.dynamodb.DynamoDbComparisonType;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DynamoDbHeuristicsCalculatorUnitTest {

    private static final double DELTA = 1.0E-6;

    private final DynamoDbHeuristicsCalculator calculator = new DynamoDbHeuristicsCalculator();

    @Test
    public void testComparisonTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Object.class, DynamoDbComparisonType.class };
        Truthness nullOperator = this.invokePrivate(this.calculator, "comparisonTruthness", parameterTypes, 10L, 10L,
                null);
        Truthness bothNull = this.invokePrivate(this.calculator, "comparisonTruthness", parameterTypes, null, null,
                DynamoDbComparisonType.EQUALS);
        Truthness oneNull = this.invokePrivate(this.calculator, "comparisonTruthness", parameterTypes, 10L, null,
                DynamoDbComparisonType.EQUALS);
        Truthness exact = this.invokePrivate(this.calculator, "comparisonTruthness", parameterTypes, 10L, 10L,
                DynamoDbComparisonType.EQUALS);
        Truthness nearMiss = this.invokePrivate(this.calculator, "comparisonTruthness", parameterTypes, 10L, 11L,
                DynamoDbComparisonType.EQUALS);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nullOperator);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, bothNull);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS_BETTER, oneNull);
        Assertions.assertTrue(exact.isTrue());
        this.assertTruthness(TruthnessUtils.buildScaledTruthness(0.15000000000000002,
                TruthnessUtils.getEqualityTruthness(10.0d, 11.0d).getOfTrue()), nearMiss);
    }

    @Test
    public void testUnscaledComparisonTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Object.class, DynamoDbComparisonType.class };
        Truthness stringEquality = this.invokePrivate(this.calculator, "unscaledComparisonTruthness", parameterTypes,
                "Argentina", "Australia", DynamoDbComparisonType.EQUALS);
        Truthness numberEquality = this.invokePrivate(this.calculator, "unscaledComparisonTruthness", parameterTypes,
                10L, 11L, DynamoDbComparisonType.EQUALS);
        Truthness numberInequality = this.invokePrivate(this.calculator, "unscaledComparisonTruthness", parameterTypes,
                10L, 11L, DynamoDbComparisonType.NOT_EQUALS);
        Truthness ordering = this.invokePrivate(this.calculator, "unscaledComparisonTruthness", parameterTypes, 10L, 8L,
                DynamoDbComparisonType.GREATER_THAN);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Argentina", "Australia"), stringEquality);
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(10.0d, 11.0d), numberEquality);
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(10.0d, 11.0d).invert(), numberInequality);
        this.assertTruthness(TruthnessUtils.getLessThanTruthness(0.0d, 2.0d), ordering);
    }

    @Test
    public void testUnscaledStringComparisonTruthness() {
        TaintHandler taintHandler = Mockito.mock(TaintHandler.class);
        DynamoDbHeuristicsCalculator taintedCalculator = new DynamoDbHeuristicsCalculator(taintHandler);
        Class<?>[] parameterTypes = new Class<?>[] { String.class, String.class, DynamoDbComparisonType.class };
        Truthness equality = this.invokePrivate(taintedCalculator, "unscaledStringComparisonTruthness", parameterTypes,
                "Argentina", "Australia", DynamoDbComparisonType.EQUALS);
        Truthness inequality = this.invokePrivate(taintedCalculator, "unscaledStringComparisonTruthness",
                parameterTypes, "Argentina", "Australia", DynamoDbComparisonType.NOT_EQUALS);
        Truthness greaterThan = this.invokePrivate(taintedCalculator, "unscaledStringComparisonTruthness",
                parameterTypes, "Uruguay", "Argentina", DynamoDbComparisonType.GREATER_THAN);
        Truthness greaterThanEquals = this.invokePrivate(taintedCalculator, "unscaledStringComparisonTruthness",
                parameterTypes, "Argentina", "Argentina", DynamoDbComparisonType.GREATER_THAN_EQUALS);
        Truthness lessThan = this.invokePrivate(taintedCalculator, "unscaledStringComparisonTruthness", parameterTypes,
                "Argentina", "Uruguay", DynamoDbComparisonType.LESS_THAN);
        Truthness lessThanEquals = this.invokePrivate(taintedCalculator, "unscaledStringComparisonTruthness",
                parameterTypes, "Argentina", "Argentina", DynamoDbComparisonType.LESS_THAN_EQUALS);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Argentina", "Australia"), equality);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Argentina", "Australia").invert(), inequality);
        Assertions.assertTrue(greaterThan.isTrue());
        Assertions.assertTrue(greaterThanEquals.isTrue());
        Assertions.assertTrue(lessThan.isTrue());
        Assertions.assertTrue(lessThanEquals.isTrue());
        (Mockito.verify(taintHandler)).handleTaintForStringEquals("Argentina", "Australia", false);
        Mockito.verifyNoMoreInteractions(taintHandler);
    }

    @Test
    public void testUnscaledEqualityTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Object.class };
        UUID messiId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID otherId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        Truthness number = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes, 10L, 11L);
        Truthness nonfinite = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes,
                Double.POSITIVE_INFINITY, 10.0d);
        Truthness bool = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes, true, false);
        Truthness binary = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes,
                new byte[] { 1, 0 }, new byte[] { 1, 1 });
        Truthness uuid = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes, messiId,
                otherId);
        Truthness equalPlayers = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes,
                new Player("Lionel Messi"), new Player("Lionel Messi"));
        Truthness differentPlayers = this.invokePrivate(this.calculator, "unscaledEqualityTruthness", parameterTypes,
                new Player("Lionel Messi"), new Player("Diego Maradona"));
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(10.0d, 11.0d), number);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nonfinite);
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(true, false), bool);
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(new byte[] { 1, 0 }, new byte[] { 1, 1 }), binary);
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(messiId, otherId), uuid);
        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, equalPlayers);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, differentPlayers);
    }

    @Test
    public void testUnscaledOrderingTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Object.class, DynamoDbComparisonType.class };
        Truthness greaterThan = this.invokePrivate(this.calculator, "unscaledOrderingTruthness", parameterTypes, 10L,
                8L, DynamoDbComparisonType.GREATER_THAN);
        Truthness greaterThanEquals = this.invokePrivate(this.calculator, "unscaledOrderingTruthness", parameterTypes,
                10L, 10L, DynamoDbComparisonType.GREATER_THAN_EQUALS);
        Truthness lessThan = this.invokePrivate(this.calculator, "unscaledOrderingTruthness", parameterTypes, 8L, 10L,
                DynamoDbComparisonType.LESS_THAN);
        Truthness lessThanEquals = this.invokePrivate(this.calculator, "unscaledOrderingTruthness", parameterTypes, 10L,
                10L, DynamoDbComparisonType.LESS_THAN_EQUALS);
        Truthness unsupported = this.invokePrivate(this.calculator, "unscaledOrderingTruthness", parameterTypes,
                new Player("Lionel Messi"), new Player("Diego Maradona"), DynamoDbComparisonType.GREATER_THAN);
        Truthness nonfinite = this.invokePrivate(this.calculator, "unscaledOrderingTruthness", parameterTypes,
                Double.POSITIVE_INFINITY, 10.0d, DynamoDbComparisonType.GREATER_THAN);
        this.assertTruthness(TruthnessUtils.getLessThanTruthness(0.0d, 2.0d), greaterThan);
        this.assertTruthness(TruthnessUtils.getLessThanTruthness(0.0d, 0.0d).invert(), greaterThanEquals);
        this.assertTruthness(TruthnessUtils.getLessThanTruthness(-2.0d, 0.0d), lessThan);
        this.assertTruthness(TruthnessUtils.getLessThanTruthness(0.0d, 0.0d).invert(), lessThanEquals);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, unsupported);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nonfinite);
    }

    @Test
    public void testMissingPathTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { String.class, Map.class };
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("country", "Argentina");
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("profile", profile);
        Truthness nullPath = this.invokePrivate(this.calculator, "missingPathTruthness", parameterTypes, null, player);
        Truthness emptyItem = this.invokePrivate(this.calculator, "missingPathTruthness", parameterTypes,
                "profile.countri", Collections.emptyMap());
        Truthness closestPath = this.invokePrivate(this.calculator, "missingPathTruthness", parameterTypes,
                "profile.countri", player);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nullPath);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, emptyItem);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("profile.country", "profile.countri"),
                closestPath);
    }

    @Test
    public void testCollectionMembershipTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Collection.class };
        List<String> candidates = Arrays.asList("Brazil", "France");
        Truthness nullCandidates = this.invokePrivate(this.calculator, "collectionMembershipTruthness", parameterTypes,
                "Argentina", null);
        Truthness emptyCandidates = this.invokePrivate(this.calculator, "collectionMembershipTruthness", parameterTypes,
                "Argentina", Collections.emptyList());
        Truthness scalar = this.invokePrivate(this.calculator, "collectionMembershipTruthness", parameterTypes,
                "Argentina", candidates);
        Truthness collection = this.invokePrivate(this.calculator, "collectionMembershipTruthness", parameterTypes,
                Arrays.asList("Argentina", "France"), candidates);
        Truthness emptyActual = this.invokePrivate(this.calculator, "collectionMembershipTruthness", parameterTypes,
                Collections.emptyList(), candidates);
        Truthness argentinaBrazil = this.scaledStringEquality("Argentina", "Brazil");
        Truthness argentinaFrance = this.scaledStringEquality("Argentina", "France");
        Truthness franceBrazil = this.scaledStringEquality("France", "Brazil");
        Truthness franceFrance = TruthnessUtils.TRUE_TRUTHNESS;
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nullCandidates);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, emptyCandidates);
        this.assertTruthness(TruthnessUtils.buildOrAggregationTruthness(argentinaBrazil, argentinaFrance), scalar);
        this.assertTruthness(TruthnessUtils.buildOrAggregationTruthness(argentinaBrazil, argentinaFrance, franceBrazil,
                franceFrance), collection);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, emptyActual);
    }

    @Test
    public void testInTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Collection.class };
        Truthness nullCandidates = this.invokePrivate(this.calculator, "inTruthness", parameterTypes, 10L, null);
        Truthness emptyCandidates = this.invokePrivate(this.calculator, "inTruthness", parameterTypes, 10L,
                Collections.emptyList());
        Truthness exactScalar = this.invokePrivate(this.calculator, "inTruthness", parameterTypes, 10L,
                Arrays.asList(7L, 10L));
        Truthness exactCollection = this.invokePrivate(this.calculator, "inTruthness", parameterTypes,
                Arrays.asList("Argentina", "France"), Arrays.asList("Brazil", "France"));
        Truthness nearestCandidate = this.invokePrivate(this.calculator, "inTruthness", parameterTypes, 10L,
                Arrays.asList(9L, 40L));
        Truthness unscaledClosest = TruthnessUtils.getEqualityTruthness(10.0d, 9.0d);
        Truthness scaledClosest = TruthnessUtils.buildScaledTruthness(0.15000000000000002, unscaledClosest.getOfTrue());
        Truthness expectedNearest = TruthnessUtils.buildScaledTruthness(0.1, scaledClosest.getOfTrue());
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nullCandidates);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, emptyCandidates);
        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, exactScalar);
        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, exactCollection);
        this.assertTruthness(expectedNearest, nearestCandidate);
    }

    @Test
    public void testStringContainsTruthness() {
        Class<?>[] parameterTypes = new Class<?>[] { String.class, String.class };
        Truthness exact = this.invokePrivate(this.calculator, "stringContainsTruthness", parameterTypes,
                "alice@example.com", "example");
        Truthness emptySubstring = this.invokePrivate(this.calculator, "stringContainsTruthness", parameterTypes,
                "Argentina", "");
        Truthness closestWindow = this.invokePrivate(this.calculator, "stringContainsTruthness", parameterTypes,
                "xxMessi", "Messo");
        Truthness shorterSource = this.invokePrivate(this.calculator, "stringContainsTruthness", parameterTypes, "Leo",
                "Leonel");
        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, exact);
        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, emptySubstring);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Messi", "Messo"), closestWindow);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Leo", "Leonel"), shorterSource);
    }

    @Test
    public void testOrderingDifference() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class, Object.class };
        Double nullDifference = this.invokePrivate(this.calculator, "orderingDifference", parameterTypes, null, 10L);
        Double numericDifference = this.invokePrivate(this.calculator, "orderingDifference", parameterTypes, 10L, 7);
        Double comparableDifference = this.invokePrivate(this.calculator, "orderingDifference", parameterTypes,
                new Date(2000L), new Date(1000L));
        Double incompatibleClasses = this.invokePrivate(this.calculator, "orderingDifference", parameterTypes,
                new Date(2000L), "Argentina");
        Double nonComparable = this.invokePrivate(this.calculator, "orderingDifference", parameterTypes,
                new Player("Lionel Messi"), new Player("Diego Maradona"));
        Assertions.assertNull(nullDifference);
        Assertions.assertEquals(3.0d, numericDifference, DELTA);
        Assertions.assertEquals(1.0d, comparableDifference, DELTA);
        Assertions.assertNull(incompatibleClasses);
        Assertions.assertNull(nonComparable);
    }

    @Test
    public void testComputeSize() {
        Class<?>[] parameterTypes = new Class<?>[] { Object.class };
        Map<String, Object> profile = Collections.singletonMap("country", "Argentina");
        Integer nullSize = this.invokePrivate(this.calculator, "computeSize", parameterTypes, new Object[] { null });
        Integer stringSize = this.invokePrivate(this.calculator, "computeSize", parameterTypes, "Brazil");
        Integer collectionSize = this.invokePrivate(this.calculator, "computeSize", parameterTypes,
                Arrays.asList("Argentina", "France"));
        Integer mapSize = this.invokePrivate(this.calculator, "computeSize", parameterTypes, profile);
        Integer binarySize = this.invokePrivate(this.calculator, "computeSize", parameterTypes,
                (Object) new byte[] { 1, 2, 3 });
        Integer unsupportedSize = this.invokePrivate(this.calculator, "computeSize", parameterTypes,
                new Player("Lionel Messi"));
        Assertions.assertNull(nullSize);
        Assertions.assertEquals(6, stringSize);
        Assertions.assertEquals(2, collectionSize);
        Assertions.assertEquals(1, mapSize);
        Assertions.assertEquals(3, binarySize);
        Assertions.assertNull(unsupportedSize);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(DynamoDbHeuristicsCalculator target, String methodName, Class<?>[] parameterTypes,
            Object... arguments) {
        try {
            Method method = DynamoDbHeuristicsCalculator.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new AssertionError("Private method failed: " + methodName, cause);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot invoke private method: " + methodName, e);
        }
    }

    private Truthness scaledStringEquality(String actual, String expected) {
        Truthness unscaled = TruthnessUtils.getStringEqualityTruthness(actual, expected);
        return unscaled.isTrue() ? unscaled
                : TruthnessUtils.buildScaledTruthness(0.15000000000000002, unscaled.getOfTrue());
    }

    private void assertTruthness(Truthness expected, Truthness actual) {
        Assertions.assertEquals(expected.getOfTrue(), actual.getOfTrue(), DELTA);
        Assertions.assertEquals(expected.getOfFalse(), actual.getOfFalse(), DELTA);
    }

    private static class Player {

        private final String name;

        private Player(String name) {
            this.name = name;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof Player)) {
                return false;
            } else {
                Player player = (Player) other;
                return this.name.equals(player.name);
            }
        }

        public int hashCode() {
            return this.name.hashCode();
        }

    }

}
