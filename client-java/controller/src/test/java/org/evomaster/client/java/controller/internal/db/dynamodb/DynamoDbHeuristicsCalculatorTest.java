package org.evomaster.client.java.controller.internal.db.dynamodb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeType;
import org.evomaster.client.java.controller.dynamodb.DynamoDbComparisonType;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.BeginsWithOperation;
import org.evomaster.client.java.controller.dynamodb.operations.BetweenOperation;
import org.evomaster.client.java.controller.dynamodb.operations.ContainsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.ExistsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.InOperation;
import org.evomaster.client.java.controller.dynamodb.operations.NotOperation;
import org.evomaster.client.java.controller.dynamodb.operations.OrOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.SizeOperation;
import org.evomaster.client.java.controller.dynamodb.operations.TypeOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.GreaterThanEqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.GreaterThanOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.LessThanOperation;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamoDbHeuristicsCalculatorTest {

    private static final double DELTA = 0.000001d;

    private static final double FIVE_DECIMAL_TRUNCATION_DELTA = 0.00001d;

    private final DynamoDbHeuristicsCalculator calculator = new DynamoDbHeuristicsCalculator();

    @Test
    public void testInvalidEntryPointsAndUnsupportedOperations() {
        Map<String, Object> player = this.item("shirt", "ten");
        Assertions.assertTrue(this.calculator.computeExpression(null, player).isFalse());
        Assertions.assertTrue(this.calculator.computeExpression(new EqualsOperation<>("shirt", "ten"), null).isFalse());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.calculator.computeExpression(new QueryOperation() {
                }, player).isFalse());
        Assertions.assertTrue(
                this.calculator.computeExpression(new AndOperation(Collections.emptyList()), player).isTrue());
        Assertions.assertTrue(
                this.calculator.computeExpression(new OrOperation(Collections.emptyList()), player).isFalse());
    }

    @Test
    public void testApplySpecifiedNullTruthnessForComparison() {
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("empty", null);
        player.put("age", 30L);

        for (DynamoDbComparisonType comparisonType : DynamoDbComparisonType.values()) {
            Truthness bothNull = this.calculator.computeExpression(comparisonType.toOperation("empty", null), player);
            Truthness nullActual = this.calculator.computeExpression(comparisonType.toOperation("empty", 10L), player);
            Truthness nullExpected = this.calculator.computeExpression(comparisonType.toOperation("age", null), player);
            this.assertTruthness(TruthnessUtils.FALSE_TRUTHNESS, bothNull);
            this.assertTruthness(TruthnessUtils.FALSE_TRUTHNESS_BETTER, nullActual);
            this.assertTruthness(TruthnessUtils.FALSE_TRUTHNESS_BETTER, nullExpected);
        }

    }

    @Test
    public void testScaleEveryFalseUnscaledComparison() {
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.EQUALS, 30L, 35L,
                TruthnessUtils.getEqualityTruthness(30.0d, 35.0d));
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.NOT_EQUALS, 30L, 30L,
                TruthnessUtils.getEqualityTruthness(30.0d, 30.0d).invert());
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.GREATER_THAN, 30L, 35L,
                TruthnessUtils.getLessThanTruthness(35.0d, 30.0d));
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.GREATER_THAN_EQUALS, 30L, 35L,
                TruthnessUtils.getLessThanTruthness(30.0d, 35.0d).invert());
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.LESS_THAN, 30L, 25L,
                TruthnessUtils.getLessThanTruthness(30.0d, 25.0d));
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.LESS_THAN_EQUALS, 30L, 25L,
                TruthnessUtils.getLessThanTruthness(25.0d, 30.0d).invert());
    }

    @Test
    public void testPreserveEveryTrueUnscaledComparison() {
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.EQUALS, 30L, 30L,
                TruthnessUtils.getEqualityTruthness(30.0d, 30.0d));
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.NOT_EQUALS, 30L, 35L,
                TruthnessUtils.getEqualityTruthness(30.0d, 35.0d).invert());
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.GREATER_THAN, 35L, 30L,
                TruthnessUtils.getLessThanTruthness(30.0d, 35.0d));
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.GREATER_THAN_EQUALS, 30L, 30L,
                TruthnessUtils.getLessThanTruthness(30.0d, 30.0d).invert());
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.LESS_THAN, 25L, 30L,
                TruthnessUtils.getLessThanTruthness(25.0d, 30.0d));
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.LESS_THAN_EQUALS, 30L, 30L,
                TruthnessUtils.getLessThanTruthness(30.0d, 30.0d).invert());
    }

    @Test
    public void testUseTypeSpecificEqualityScoresForNonNumericScalars() {
        RecordingTaintHandler taintHandler = new RecordingTaintHandler();
        DynamoDbHeuristicsCalculator taintedCalculator = new DynamoDbHeuristicsCalculator(taintHandler);
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("country", "Argentina");
        player.put("captain", true);
        player.put("photo", new byte[] { 1, 2, 3 });
        player.put("id", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        Truthness equalCountry = taintedCalculator.computeExpression(new EqualsOperation<>("country", "Argentina"),
                player);
        Truthness nearCountry = taintedCalculator.computeExpression(new EqualsOperation<>("country", "Australia"),
                player);
        Truthness farCountry = taintedCalculator.computeExpression(new EqualsOperation<>("country", "Zimbabwe"),
                player);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Argentina", "Argentina"), equalCountry);
        this.assertTruthness(this.scaledTruthness(TruthnessUtils.getStringEqualityTruthness("Argentina", "Australia")),
                nearCountry);
        this.assertTruthness(this.scaledTruthness(TruthnessUtils.getStringEqualityTruthness("Argentina", "Zimbabwe")),
                farCountry);
        Assertions.assertTrue(nearCountry.getOfTrue() > farCountry.getOfTrue());
        Assertions.assertEquals(3, taintHandler.calls);

        Truthness equalCaptain = this.calculator.computeExpression(new EqualsOperation<>("captain", true), player);
        Truthness unequalCaptain = this.calculator.computeExpression(new EqualsOperation<>("captain", false), player);
        this.assertTruthness(TruthnessUtils.TRUE_TRUTHNESS, equalCaptain);
        this.assertTruthness(this.scaledTruthness(TruthnessUtils.getEqualityTruthness(true, false)), unequalCaptain);

        Truthness equalPhoto = this.calculator.computeExpression(new EqualsOperation<>("photo", new byte[] { 1, 2, 3 }),
                player);
        Truthness nearPhoto = this.calculator.computeExpression(new EqualsOperation<>("photo", new byte[] { 1, 2, 4 }),
                player);
        Truthness farPhoto = this.calculator.computeExpression(new EqualsOperation<>("photo", new byte[] { 9, 8, 7 }),
                player);
        this.assertTruthness(TruthnessUtils.TRUE_TRUTHNESS, equalPhoto);
        this.assertTruthness(
                this.scaledTruthness(
                        TruthnessUtils.getEqualityTruthness(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 4 })), nearPhoto);
        this.assertTruthness(this.scaledTruthness(
                TruthnessUtils.getEqualityTruthness(new byte[] { 1, 2, 3 }, new byte[] { 9, 8, 7 })), farPhoto);
        Assertions.assertTrue(nearPhoto.getOfTrue() > farPhoto.getOfTrue());
        UUID sameId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID otherId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        Truthness equalId = this.calculator.computeExpression(new EqualsOperation<>("id", sameId), player);
        Truthness unequalId = this.calculator.computeExpression(new EqualsOperation<>("id", otherId), player);
        this.assertTruthness(TruthnessUtils.getEqualityTruthness(sameId, sameId), equalId);
        this.assertTruthness(this.scaledTruthness(TruthnessUtils.getEqualityTruthness(sameId, otherId)), unequalId);
    }

    @Test
    public void testReturnTrueTruthnessForExactEquality() {
        this.computeAndAssertTrueEquality(10L, 10L);
        this.computeAndAssertTrueEquality(10, 10L);
        this.computeAndAssertTrueEquality("Argentina", "Argentina");
        this.computeAndAssertTrueEquality(true, true);
        this.computeAndAssertTrueEquality(new byte[] { 1, 0 }, new byte[] { 1, 0 });
        UUID playerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        this.computeAndAssertTrueEquality(playerId, playerId);
        Object player = new Object();
        this.computeAndAssertTrueEquality(player, player);
        Truthness matchingCondition = this.calculator.computeCondition(new EqualsOperation<>("score", 10L),
                Collections.singletonList(this.item("score", 10L)));
        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, matchingCondition);
        Truthness nonfinite = this.calculator.computeExpression(
                new EqualsOperation<>("score", Double.POSITIVE_INFINITY), this.item("score", Double.POSITIVE_INFINITY));
        this.assertTruthness(this.scaledTruthness(TruthnessUtils.FALSE_TRUTHNESS), nonfinite);
    }

    @Test
    public void testMatchAdvancedSqlStringComparisonTruthness() {
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.EQUALS, "Argentina", "Argentina",
                TruthnessUtils.getStringEqualityTruthness("Argentina", "Argentina"));
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.EQUALS, "Argentina", "Australia",
                TruthnessUtils.getStringEqualityTruthness("Argentina", "Australia"));
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.NOT_EQUALS, "Argentina", "Australia",
                TruthnessUtils.getStringEqualityTruthness("Argentina", "Australia").invert());
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.NOT_EQUALS, "Argentina", "Argentina",
                TruthnessUtils.getStringEqualityTruthness("Argentina", "Argentina").invert());
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.GREATER_THAN, "Zimbabwe", "Argentina",
                TruthnessUtils.TRUE_TRUTHNESS);
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.GREATER_THAN_EQUALS, "Argentina", "Argentina",
                TruthnessUtils.TRUE_TRUTHNESS);
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.LESS_THAN, "Argentina", "Zimbabwe",
                TruthnessUtils.TRUE_TRUTHNESS);
        this.computeAndAssertUnscaledComparison(DynamoDbComparisonType.LESS_THAN_EQUALS, "Argentina", "Argentina",
                TruthnessUtils.TRUE_TRUTHNESS);
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.GREATER_THAN, "Argentina", "Australia",
                TruthnessUtils.FALSE_TRUTHNESS);
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.GREATER_THAN_EQUALS, "Argentina", "Australia",
                TruthnessUtils.FALSE_TRUTHNESS);
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.LESS_THAN, "Zimbabwe", "Argentina",
                TruthnessUtils.FALSE_TRUTHNESS);
        this.computeAndAssertScaledComparison(DynamoDbComparisonType.LESS_THAN_EQUALS, "Zimbabwe", "Argentina",
                TruthnessUtils.FALSE_TRUTHNESS);
    }

    @Test
    public void testKeepStringOrderingFlatAndOnlyTaintEquality() {
        RecordingTaintHandler taintHandler = new RecordingTaintHandler();
        DynamoDbHeuristicsCalculator taintedCalculator = new DynamoDbHeuristicsCalculator(taintHandler);
        Map<String, Object> player = this.item("country", "Argentina");
        Truthness nearOrderingMiss = taintedCalculator
            .computeExpression(DynamoDbComparisonType.GREATER_THAN.toOperation("country", "Australia"), player);
        Truthness farOrderingMiss = taintedCalculator
            .computeExpression(DynamoDbComparisonType.GREATER_THAN.toOperation("country", "Zimbabwe"), player);
        taintedCalculator.computeExpression(DynamoDbComparisonType.NOT_EQUALS.toOperation("country", "Australia"),
                player);
        taintedCalculator.computeExpression(DynamoDbComparisonType.EQUALS.toOperation("country", "Argentina"), player);
        Truthness expectedOrderingMiss = this.scaledTruthness(TruthnessUtils.FALSE_TRUTHNESS);
        this.assertTruthness(expectedOrderingMiss, nearOrderingMiss);
        this.assertTruthness(expectedOrderingMiss, farOrderingMiss);
        Assertions.assertEquals(1, taintHandler.calls);
    }

    @Test
    public void testKeepDistanceBasedRankingForPrefixContainmentAndMissingFields() {
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("email", "alice@example.com");
        player.put("shortName", "Al");
        player.put("team", "Argentina");
        player.put("skills", Arrays.asList("java", "aws"));
        player.put("shirt", 10L);

        Truthness exactPrefix = this.calculator.computeExpression(new BeginsWithOperation("email", "alice@"), player);
        Truthness nearPrefix = this.calculator.computeExpression(new BeginsWithOperation("email", "alicia@"), player);
        Truthness farPrefix = this.calculator.computeExpression(new BeginsWithOperation("email", "zzzzzz@"), player);
        Truthness sourceShorterThanPrefix = this.calculator
            .computeExpression(new BeginsWithOperation("shortName", "Alice"), player);
        Truthness incompatiblePrefixSource = this.calculator.computeExpression(new BeginsWithOperation("shirt", "1"),
                player);
        Assertions.assertTrue(exactPrefix.isTrue());
        Assertions.assertTrue(nearPrefix.isFalse());
        Assertions.assertTrue(farPrefix.isFalse());
        Assertions.assertTrue(sourceShorterThanPrefix.isFalse());
        Assertions.assertTrue(incompatiblePrefixSource.isFalse());
        Assertions.assertTrue(nearPrefix.getOfTrue() > farPrefix.getOfTrue());

        Truthness nearContains = this.calculator.computeExpression(new ContainsOperation("team", "Argntina"), player);
        Truthness farContains = this.calculator.computeExpression(new ContainsOperation("team", "Zimbabwe"), player);
        Truthness exactContains = this.calculator.computeExpression(new ContainsOperation("email", "example"), player);
        Truthness unsupportedContains = this.calculator.computeExpression(new ContainsOperation("shirt", 7L), player);
        Assertions.assertTrue(nearContains.isFalse());
        Assertions.assertTrue(farContains.isFalse());
        Assertions.assertTrue(exactContains.isTrue());
        Assertions.assertTrue(unsupportedContains.isFalse());
        Assertions.assertTrue(nearContains.getOfTrue() > farContains.getOfTrue());

        Truthness nearMissing = this.calculator.computeExpression(new ExistsOperation("emial", true), player);
        Truthness farMissing = this.calculator.computeExpression(new ExistsOperation("zzzzz", true), player);
        Assertions.assertTrue(nearMissing.isFalse());
        Assertions.assertTrue(farMissing.isFalse());
        Assertions.assertTrue(nearMissing.getOfTrue() > farMissing.getOfTrue());
    }

    @Test
    public void testReturnCanonicalFalseForMissingNonExistencePredicatePaths() {
        Map<String, Object> player = this.item("name", "Lionel Messi", "age", 36L);

        for (QueryOperation operation : Arrays.asList(new EqualsOperation<>("missing", "Argentina"),
                new GreaterThanOperation<>("missing", 30L), new BetweenOperation("missing", 30L, 40L),
                new InOperation<>("missing", Arrays.asList("Argentina", "France")),
                new TypeOperation("missing", DynamoDbAttributeType.STRING),
                new BeginsWithOperation("missing", "Lionel"), new ContainsOperation("missing", "Messi"),
                new SizeOperation("missing", DynamoDbComparisonType.EQUALS, 1L))) {
            Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, this.calculator.computeExpression(operation, player));
        }

        Assertions.assertSame(TruthnessUtils.TRUE_TRUTHNESS, this.calculator
            .computeExpression(DynamoDbComparisonType.NOT_EQUALS.toOperation("missing", 15L), player));
    }

    @Test
    public void testMaximizeStringEqualityTruthnessForContainsCandidates() {
        Truthness bestWindow = this.calculator.computeExpression(new ContainsOperation("name", "Messo"),
                this.item("name", "xxMessi"));
        Truthness shorterSource = this.calculator.computeExpression(new ContainsOperation("name", "Leonel"),
                this.item("name", "Leo"));
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Messi", "Messo"), bestWindow);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("Leo", "Leonel"), shorterSource);
    }

    @Test
    public void testApplyComparisonRulesToBetweenAndSize() {
        Map<String, Object> team = this.item("score", 50L, "players",
                Arrays.asList("Lionel Messi", "Julian Alvarez", "Lautaro Martinez"), "unknownPlayers", null, "country",
                "Brazil", "profile", Collections.singletonMap("coach", "Carlo Ancelotti"), "captain", true, "payload",
                new byte[] { 1, 2, 3, 4 });
        Truthness expectedLower = TruthnessUtils.getLessThanTruthness(50.0d, 35.0d).invert();
        Truthness expectedUpper = this.scaledTruthness(TruthnessUtils.getLessThanTruthness(40.0d, 50.0d).invert());
        Truthness expectedBetween = TruthnessUtils.buildAndAggregationTruthness(expectedLower, expectedUpper);
        Truthness actualBetween = this.calculator.computeExpression(new BetweenOperation("score", 35L, 40L), team);
        Truthness expectedSize = this.scaledTruthness(TruthnessUtils.getLessThanTruthness(4.0d, 3.0d));
        Truthness actualSize = this.calculator
            .computeExpression(new SizeOperation("players", DynamoDbComparisonType.GREATER_THAN, 4L), team);
        Truthness nullSize = this.calculator
            .computeExpression(new SizeOperation("unknownPlayers", DynamoDbComparisonType.GREATER_THAN, 4L), team);
        Truthness stringSize = this.calculator
            .computeExpression(new SizeOperation("country", DynamoDbComparisonType.EQUALS, 6L), team);
        Truthness mapSize = this.calculator
            .computeExpression(new SizeOperation("profile", DynamoDbComparisonType.EQUALS, 1L), team);
        Truthness binarySize = this.calculator
            .computeExpression(new SizeOperation("payload", DynamoDbComparisonType.EQUALS, 4L), team);
        Truthness unsupportedSize = this.calculator
            .computeExpression(new SizeOperation("captain", DynamoDbComparisonType.EQUALS, 1L), team);
        this.assertTruthness(expectedBetween, actualBetween);
        this.assertTruthness(expectedSize, actualSize);
        this.assertTruthness(TruthnessUtils.FALSE_TRUTHNESS_BETTER, nullSize);
        Assertions.assertTrue(stringSize.isTrue());
        Assertions.assertTrue(mapSize.isTrue());
        Assertions.assertTrue(binarySize.isTrue());
        Assertions.assertTrue(unsupportedSize.isFalse());
    }

    @Test
    public void testScaleTheBestCandidateForHCondition() {
        QueryOperation condition = new EqualsOperation<>("age", 30L);
        Collection<Map<String, Object>> players = Arrays.asList(this.item("name", "Lionel Messi", "age", 36L),
                this.item("name", "Jude Bellingham", "age", 29L), this.item("name", "Luka Modric", "age", 38L));
        Truthness bestRow = this.calculator.computeExpression(condition,
                this.item("name", "Jude Bellingham", "age", 29L));
        Truthness expected = TruthnessUtils.buildScaledTruthness(0.1, bestRow.getOfTrue());
        this.assertTruthness(expected, this.calculator.computeCondition(condition, players));
    }

    @Test
    public void testSeparateKeyAccessFromFilteringAndOnlyFilterCandidates() {
        Collection<Map<String, Object>> players = Arrays.asList(
                this.item("name", "Kylian Mbappe", "country", "France", "goals", 9L),
                this.item("name", "Antoine Griezmann", "country", "France", "goals", 8L),
                this.item("name", "Vinicius Junior", "country", "Brazil", "goals", 100L));
        QueryOperation keyCondition = new EqualsOperation<>("country", "France");
        QueryOperation filterCondition = new GreaterThanOperation<>("goals", 10L);
        Truthness keyScore = this.calculator.computeCondition(keyCondition, players);
        Collection<Map<String, Object>> frenchPlayers = Arrays.asList(
                this.item("name", "Kylian Mbappe", "country", "France", "goals", 9L),
                this.item("name", "Antoine Griezmann", "country", "France", "goals", 8L));
        Truthness filterScore = this.calculator.computeCondition(filterCondition, frenchPlayers);
        Truthness expected = TruthnessUtils.buildAndAggregationTruthness(keyScore, filterScore);
        Truthness actual = this.calculator.computeQuery(keyCondition, filterCondition, players);
        Assertions.assertTrue(actual.isFalse());
        this.assertTruthness(expected, actual);
    }

    @Test
    public void testAggregateFalseTruthnessWhenKeyConditionHasNoCandidates() {
        Collection<Map<String, Object>> players = Arrays.asList(
                this.item("name", "Lionel Messi", "country", "Argentina", "goals", 13L),
                this.item("name", "Kylian Mbappe", "country", "France", "goals", 9L));
        QueryOperation keyCondition = new EqualsOperation<>("country", "Spain");
        QueryOperation filterCondition = new GreaterThanOperation<>("goals", 10L);
        Truthness keyScore = this.calculator.computeCondition(keyCondition, players);
        Truthness expected = TruthnessUtils.buildAndAggregationTruthness(keyScore, TruthnessUtils.FALSE_TRUTHNESS);
        Truthness actual = this.calculator.computeQuery(keyCondition, filterCondition, players);
        Assertions.assertTrue(actual.isFalse());
        this.assertTruthness(expected, actual);
    }

    @Test
    public void testPreferFilterFailureOverKeyAccessFailure() {
        Collection<Map<String, Object>> players = Arrays.asList(
                this.item("name", "Kylian Mbappe", "country", "France", "goals", 9L),
                this.item("name", "Antoine Griezmann", "country", "France", "goals", 8L),
                this.item("name", "Vinicius Junior", "country", "Brazil", "goals", 12L));
        QueryOperation filterCondition = new GreaterThanOperation<>("goals", 20L);
        Truthness keyFailure = this.calculator.computeQuery(new EqualsOperation<>("country", "Spain"), filterCondition,
                players);
        Truthness filterFailure = this.calculator.computeQuery(new EqualsOperation<>("country", "France"),
                filterCondition, players);
        Assertions.assertTrue(keyFailure.isFalse());
        Assertions.assertTrue(filterFailure.isFalse());
        Assertions.assertTrue(filterFailure.getOfTrue() > keyFailure.getOfTrue());
        Assertions.assertTrue(this.calculator.computeDistance(new EqualsOperation<>("country", "France"),
                filterCondition, players) < this.calculator.computeDistance(new EqualsOperation<>("country", "Spain"),
                        filterCondition, players));
    }

    @Test
    public void testSpecificationKeyNotFoundExampleCalculation() {
        List<Map<String, Object>> orders = this.getExampleOrders();
        EqualsOperation<String> customerIdEquals = new EqualsOperation<>("customerId", "cust-125");
        GreaterThanEqualsOperation<String> orderDateAtOrAfter = new GreaterThanEqualsOperation<>("orderDate",
                "2024-01-01");
        QueryOperation keyCondition = new AndOperation(Arrays.asList(customerIdEquals, orderDateAtOrAfter));
        QueryOperation filterCondition = this.getExampleFilterCondition();
        Truthness itemACustomer = this.calculator.computeExpression(customerIdEquals, orders.get(0));
        Truthness itemBCustomer = this.calculator.computeExpression(customerIdEquals, orders.get(1));
        Truthness itemCCustomer = this.calculator.computeExpression(customerIdEquals, orders.get(2));
        this.assertCalculation("scaleTrue(C_BETTER, eq(cust-124, cust-125)) for Item A", 0.6175, 1.0d, itemACustomer);
        this.assertCalculation("scaleTrue(C_BETTER, eq(cust-124, cust-125)) for Item B", 0.6175, 1.0d, itemBCustomer);
        this.assertCalculation("scaleTrue(C_BETTER, eq(cust-999, cust-125)) for Item C", 0.27325, 1.0d, itemCCustomer);

        Truthness itemADate = this.calculator.computeExpression(orderDateAtOrAfter, orders.get(0));
        Truthness itemBDate = this.calculator.computeExpression(orderDateAtOrAfter, orders.get(1));
        Truthness itemCDate = this.calculator.computeExpression(orderDateAtOrAfter, orders.get(2));
        this.assertCalculation("ge(2024-01-10, 2024-01-01)", 1.0d, 0.1, itemADate);
        this.assertCalculation("ge(2024-01-12, 2024-01-01) for Item B", 1.0d, 0.1, itemBDate);
        this.assertCalculation("ge(2024-01-12, 2024-01-01) for Item C", 1.0d, 0.1, itemCDate);

        Truthness itemAKey = this.calculator.computeExpression(keyCondition, orders.get(0));
        Truthness itemBKey = this.calculator.computeExpression(keyCondition, orders.get(1));
        Truthness itemCKey = this.calculator.computeExpression(keyCondition, orders.get(2));
        this.assertCalculation("andAggregation(Item A customer, Item A orderDate)", 0.80875, 1.0d, itemAKey);
        this.assertCalculation("andAggregation(Item B customer, Item B orderDate)", 0.80875, 1.0d, itemBKey);
        this.assertCalculation("andAggregation(Item C customer, Item C orderDate)", 0.636625, 1.0d, itemCKey);
        double maximumItemKeyScore = Math.max(itemAKey.getOfTrue(),
                Math.max(itemBKey.getOfTrue(), itemCKey.getOfTrue()));
        Assertions.assertEquals(0.80875, maximumItemKeyScore, DELTA, "maxOfTrue(DB, K)");

        Truthness keyScore = this.calculator.computeCondition(keyCondition, orders);
        this.assertCalculation("scaleTrue(C, maxOfTrue(DB, K))", 0.827875, 1.0d, keyScore);
        Truthness emptyFilterScore = this.calculator.computeCondition(filterCondition, Collections.emptyList());
        this.assertCalculation("H-Condition(empty candidates, F)", 0.1, 1.0d, emptyFilterScore);
        Truthness queryScore = this.calculator.computeQuery(keyCondition, filterCondition, orders);
        this.assertCalculation("andAggregation(keyScore, emptyFilterScore)", 0.4639375, 1.0d, queryScore);
        Assertions.assertEquals(0.5360625, this.calculator.computeDistance(keyCondition, filterCondition, orders),
                DELTA, "DDB distance = 1 - H-Query(DB, K, F).ofTrue");
    }

    @Test
    public void testSpecificationFilterExampleCalculation() {
        List<Map<String, Object>> orders = this.getExampleOrders();
        EqualsOperation<String> customerIdEquals = new EqualsOperation<>("customerId", "cust-124");
        GreaterThanEqualsOperation<String> orderDateAtOrAfter = new GreaterThanEqualsOperation<>("orderDate",
                "2024-01-01");
        QueryOperation keyCondition = new AndOperation(Arrays.asList(customerIdEquals, orderDateAtOrAfter));
        EqualsOperation<String> statusEquals = new EqualsOperation<>("status", "OPEN");
        LessThanOperation<Long> totalLessThan = new LessThanOperation<>("total", 90L);
        QueryOperation filterCondition = new AndOperation(Arrays.asList(statusEquals, totalLessThan));
        List<Map<String, Object>> candidates = orders.subList(0, 2);

        Truthness itemAKey = this.calculator.computeExpression(keyCondition, orders.get(0));
        Truthness itemBKey = this.calculator.computeExpression(keyCondition, orders.get(1));
        Truthness itemCKey = this.calculator.computeExpression(keyCondition, orders.get(2));
        this.assertCalculation("Item A satisfies customerId and orderDate", 1.0d, 0.1, itemAKey);
        this.assertCalculation("Item B satisfies customerId and orderDate", 1.0d, 0.1, itemBKey);
        Assertions.assertTrue(itemCKey.isFalse(), "Item C must not enter Cand(DB, K)");

        Truthness keyScore = this.calculator.computeCondition(keyCondition, orders);
        this.assertCalculation("H-Condition(DB, K) with matching candidates", 1.0d, 0.1, keyScore);

        Truthness itemAStatus = this.calculator.computeExpression(statusEquals, candidates.get(0));
        Truthness itemBStatus = this.calculator.computeExpression(statusEquals, candidates.get(1));
        Truthness itemATotal = this.calculator.computeExpression(totalLessThan, candidates.get(0));
        Truthness itemBTotal = this.calculator.computeExpression(totalLessThan, candidates.get(1));
        this.assertCalculation("eq(OPEN, OPEN) after comparison scaling", 1.0d, 0.1, itemAStatus);
        this.assertCalculation("scaleTrue(C_BETTER, eq(CLOSED, OPEN))", 0.235, 1.0d, itemBStatus,
                FIVE_DECIMAL_TRUNCATION_DELTA);
        this.assertCalculation("scaleTrue(C_BETTER, lt(95, 90))", 0.28934, 1.0d, itemATotal,
                FIVE_DECIMAL_TRUNCATION_DELTA);
        this.assertCalculation("scaleTrue(C_BETTER, lt(105, 90))", 0.20279, 1.0d, itemBTotal,
                FIVE_DECIMAL_TRUNCATION_DELTA);

        Truthness itemAFilter = this.calculator.computeExpression(filterCondition, candidates.get(0));
        Truthness itemBFilter = this.calculator.computeExpression(filterCondition, candidates.get(1));
        this.assertCalculation("andAggregation(Item A status, Item A total)", 0.64467, 1.0d, itemAFilter,
                FIVE_DECIMAL_TRUNCATION_DELTA);
        this.assertCalculation("andAggregation(Item B status, Item B total)", 0.2189, 1.0d, itemBFilter,
                FIVE_DECIMAL_TRUNCATION_DELTA);
        double maximumCandidateFilterScore = Math.max(itemAFilter.getOfTrue(), itemBFilter.getOfTrue());
        Assertions.assertEquals(0.64467, maximumCandidateFilterScore, FIVE_DECIMAL_TRUNCATION_DELTA,
                "maxOfTrue(Cand(DB, K), F)");

        Truthness filterScore = this.calculator.computeCondition(filterCondition, candidates);
        this.assertCalculation("scaleTrue(C, maxOfTrue(Cand(DB, K), F))", 0.6802, 1.0d, filterScore,
                FIVE_DECIMAL_TRUNCATION_DELTA);

        Truthness queryScore = this.calculator.computeQuery(keyCondition, filterCondition, orders);
        double filterFailureDistance = this.calculator.computeDistance(keyCondition, filterCondition, orders);
        this.assertCalculation("andAggregation(keyScore, filterScore)", 0.8401, 1.0d, queryScore,
                FIVE_DECIMAL_TRUNCATION_DELTA);
        Assertions.assertEquals(0.15989, filterFailureDistance, FIVE_DECIMAL_TRUNCATION_DELTA,
                "DDB distance = 1 - H-Query(DB, K, F).ofTrue");
        double keyNotFoundDistance = this.calculator.computeDistance(this.getExampleKeyCondition(),
                this.getExampleFilterCondition(), orders);
        Assertions.assertTrue(filterFailureDistance < keyNotFoundDistance);
    }

    @Test
    public void testUseMaximumThenScaleForInCondition() {
        Map<String, Object> player = this.item("shirt", 10L, "countries", Arrays.asList("Argentina", "France"));
        List<Long> candidates = Arrays.asList(9L, 40L);
        Truthness closest = this.calculator.computeExpression(new EqualsOperation<>("shirt", 9L), player);
        Truthness expected = TruthnessUtils.buildScaledTruthness(0.1, closest.getOfTrue());
        Truthness actual = this.calculator.computeExpression(new InOperation<>("shirt", candidates), player);
        Truthness matchingCollection = this.calculator
            .computeExpression(new InOperation<>("countries", Arrays.asList("Brazil", "France")), player);
        Truthness emptyCandidates = this.calculator
            .computeExpression(new InOperation<>("shirt", Collections.emptyList()), player);
        this.assertTruthness(expected, actual);
        Assertions.assertTrue(matchingCollection.isTrue());
        Assertions.assertTrue(emptyCandidates.isFalse());
    }

    @Test
    public void testUseSpecifiedTypeClosenessCategories() {
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("country", "Argentina");
        player.put("clubs", new LinkedHashSet<>(Arrays.asList("Barcelona", "Inter Miami")));

        Truthness scalarToSet = this.calculator
            .computeExpression(new TypeOperation("country", DynamoDbAttributeType.STRING_SET), player);
        Truthness setToScalar = this.calculator
            .computeExpression(new TypeOperation("clubs", DynamoDbAttributeType.STRING), player);
        Truthness unrelated = this.calculator
            .computeExpression(new TypeOperation("country", DynamoDbAttributeType.NUMBER), player);
        Truthness exact = this.calculator.computeExpression(new TypeOperation("country", DynamoDbAttributeType.STRING),
                player);
        Truthness nullExpectedType = this.calculator.computeExpression(new TypeOperation("country", null), player);
        Assertions.assertEquals(0.15000000000000002, scalarToSet.getOfTrue(), DELTA);
        Assertions.assertEquals(0.15000000000000002, setToScalar.getOfTrue(), DELTA);
        Assertions.assertEquals(0.1, unrelated.getOfTrue(), DELTA);
        Assertions.assertTrue(exact.isTrue());
        Assertions.assertTrue(nullExpectedType.isFalse());
    }

    @Test
    public void testUseResolvableDocumentPathsForAttributeExistsGradient() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("country", "Argentina");
        profile.put("clubs", Arrays.asList("Barcelona", "Inter Miami"));
        Map<String, Object> player = this.item("name", "Lionel Messi", "profile", profile);

        Truthness exact = this.calculator.computeExpression(new ExistsOperation("profile.country", true), player);
        Truthness nearMiss = this.calculator.computeExpression(new ExistsOperation("profile.countri", true), player);
        Truthness farMiss = this.calculator.computeExpression(new ExistsOperation("statistics.assists", true), player);
        Truthness exactNonExistence = this.calculator.computeExpression(new ExistsOperation("profile.country", false),
                player);
        Truthness missingNonExistence = this.calculator
            .computeExpression(new ExistsOperation("statistics.assists", false), player);
        Truthness nullPath = this.calculator.computeExpression(new ExistsOperation(null, true), player);
        Truthness emptyItem = this.calculator.computeExpression(new ExistsOperation("country", true),
                Collections.emptyMap());
        Assertions.assertTrue(exact.isTrue());
        Assertions.assertTrue(nearMiss.isFalse());
        Assertions.assertTrue(farMiss.isFalse());
        Assertions.assertTrue(exactNonExistence.isFalse());
        Assertions.assertTrue(missingNonExistence.isTrue());
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, nullPath);
        Assertions.assertSame(TruthnessUtils.FALSE_TRUTHNESS, emptyItem);
        this.assertTruthness(TruthnessUtils.getStringEqualityTruthness("profile.country", "profile.countri"), nearMiss);
        Assertions.assertTrue(nearMiss.getOfTrue() > farMiss.getOfTrue());
    }

    @Test
    public void testAggregateCollectionContainsUsingElementTruthness() {
        Map<String, Object> player = this.item("clubs",
                Arrays.asList("Barcelona", "Paris Saint-Germain", "Inter Miami"));
        Truthness barcelona = this.calculator.computeExpression(new EqualsOperation<>("club", "Bayern Munich"),
                this.item("club", "Barcelona"));
        Truthness paris = this.calculator.computeExpression(new EqualsOperation<>("club", "Bayern Munich"),
                this.item("club", "Paris Saint-Germain"));
        Truthness miami = this.calculator.computeExpression(new EqualsOperation<>("club", "Bayern Munich"),
                this.item("club", "Inter Miami"));
        Truthness expected = TruthnessUtils.buildOrAggregationTruthness(barcelona, paris, miami);
        Truthness actual = this.calculator.computeExpression(new ContainsOperation("clubs", "Bayern Munich"), player);
        this.assertTruthness(expected, actual);
    }

    @Test
    public void testCompareComparableAndUnsupportedRuntimeValues() {
        Map<String, Object> player = this.item("birthday", new Date(2000L), "player", new Player("Lionel Messi"));
        Truthness comparableOrdering = this.calculator
            .computeExpression(new GreaterThanOperation<>("birthday", new Date(1000L)), player);
        Truthness unsupportedOrdering = this.calculator
            .computeExpression(new GreaterThanOperation<>("player", new Player("Diego Maradona")), player);
        Truthness unequalCustomValues = this.calculator
            .computeExpression(new EqualsOperation<>("player", new Player("Diego Maradona")), player);
        Assertions.assertTrue(comparableOrdering.isTrue());
        Assertions.assertTrue(unsupportedOrdering.isFalse());
        Assertions.assertTrue(unequalCustomValues.isFalse());
    }

    @Test
    public void testInvertNestedLogicalConditions() {
        Map<String, Object> player = this.item("age", 30L, "city", "Rome");
        Truthness notAnd = this.calculator.computeExpression(
                new NotOperation(new AndOperation(
                        Arrays.asList(new EqualsOperation<>("city", "Rome"), new GreaterThanOperation<>("age", 40L)))),
                player);
        Truthness doubleNot = this.calculator
            .computeExpression(new NotOperation(new NotOperation(new EqualsOperation<>("city", "Rome"))), player);
        Truthness notOr = this.calculator.computeExpression(
                new NotOperation(new OrOperation(
                        Arrays.asList(new EqualsOperation<>("city", "Rome"), new EqualsOperation<>("city", "Paris")))),
                player);
        Assertions.assertTrue(notAnd.isTrue());
        Assertions.assertTrue(doubleNot.isTrue());
        Assertions.assertTrue(notOr.isFalse());
    }

    @Test
    public void testKeepGradientForSingleNonMatchingItem() {
        Map<String, Object> player = this.item("name", "Kylian Mbappe", "age", 25L);
        Truthness nearMiss = this.calculator.computeExpression(new EqualsOperation<>("age", 26L), player);
        Truthness farMiss = this.calculator.computeExpression(new EqualsOperation<>("age", 40L), player);
        Truthness midMiss = this.calculator.computeExpression(new EqualsOperation<>("age", 30L), player);
        Assertions.assertTrue(nearMiss.isFalse());
        Assertions.assertTrue(farMiss.isFalse());
        Assertions.assertTrue(nearMiss.getOfTrue() > farMiss.getOfTrue());
        Assertions.assertTrue(nearMiss.getOfTrue() > midMiss.getOfTrue());
        Assertions.assertTrue(midMiss.getOfTrue() > farMiss.getOfTrue());
    }

    private void assertTruthness(Truthness expected, Truthness actual) {
        Assertions.assertEquals(expected.getOfTrue(), actual.getOfTrue(), DELTA);
        Assertions.assertEquals(expected.getOfFalse(), actual.getOfFalse(), DELTA);
    }

    private void assertCalculation(String calculation, double expectedOfTrue, double expectedOfFalse,
            Truthness actual) {
        this.assertCalculation(calculation, expectedOfTrue, expectedOfFalse, actual, DELTA);
    }

    private void assertCalculation(String calculation, double expectedOfTrue, double expectedOfFalse, Truthness actual,
            double delta) {
        Assertions.assertEquals(expectedOfTrue, actual.getOfTrue(), delta, calculation + " ofTrue");
        Assertions.assertEquals(expectedOfFalse, actual.getOfFalse(), delta, calculation + " ofFalse");
    }

    private void computeAndAssertTrueEquality(Object actual, Object expected) {
        Truthness truthness = this.calculator.computeExpression(new EqualsOperation<>("value", expected),
                this.item("value", actual));
        Assertions.assertTrue(truthness.isTrue());
    }

    private void computeAndAssertScaledComparison(DynamoDbComparisonType comparisonType, Object actual, Object expected,
            Truthness unscaled) {
        Truthness truthness = this.calculator.computeExpression(comparisonType.toOperation("score", expected),
                this.item("score", actual));
        this.assertTruthness(this.scaledTruthness(unscaled), truthness);
    }

    private void computeAndAssertUnscaledComparison(DynamoDbComparisonType comparisonType, Object actual,
            Object expected, Truthness unscaled) {
        Truthness truthness = this.calculator.computeExpression(comparisonType.toOperation("score", expected),
                this.item("score", actual));
        this.assertTruthness(unscaled, truthness);
    }

    private Truthness scaledTruthness(Truthness unscaled) {
        return TruthnessUtils.buildScaledTruthness(0.15000000000000002, unscaled.getOfTrue());
    }

    private List<Map<String, Object>> getExampleOrders() {
        return Arrays.asList(
                this.item("customerId", "cust-124", "orderDate", "2024-01-10", "status", "OPEN", "total", 95L),
                this.item("customerId", "cust-124", "orderDate", "2024-01-12", "status", "CLOSED", "total", 105L),
                this.item("customerId", "cust-999", "orderDate", "2024-01-12", "status", "OPEN", "total", 50L));
    }

    private QueryOperation getExampleKeyCondition() {
        return new AndOperation(Arrays.asList(new EqualsOperation<>("customerId", "cust-125"),
                new GreaterThanEqualsOperation<>("orderDate", "2024-01-01")));
    }

    private QueryOperation getExampleFilterCondition() {
        return new AndOperation(
                Arrays.asList(new EqualsOperation<>("status", "OPEN"), new LessThanOperation<>("total", 100L)));
    }

    private Map<String, Object> item(Object... keysAndValues) {
        Map<String, Object> item = new LinkedHashMap<>();

        for (int i = 0; i < keysAndValues.length; i += 2) {
            item.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }

        return item;
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

    private static class RecordingTaintHandler implements TaintHandler {

        private int calls;

        private RecordingTaintHandler() {
        }

        public void handleTaintForStringEquals(String left, String right, boolean ignoreCase) {
            ++this.calls;
        }

        public void handleTaintForRegex(String value, String regex) {
        }

    }

}
