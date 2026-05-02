package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.*;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class DynamoDbExpressionParserTest extends DynamoDbTestBase {

    private final DynamoDbExpressionParser parser = new DynamoDbExpressionParser();

    @Test
    public void testBlankAndInvalidExpressions() {
        assertNull(parser.parse(null, Collections.emptyMap(), Collections.emptyMap()));
        assertNull(parser.parse("   ", Collections.emptyMap(), Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("id =", Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void testComparisonOperatorsAndValueKinds() {
        assertComparison(parser.parse("playerName = 'Messi'", null, null),
                EqualsOperation.class, "playerName", "Messi");

        assertComparison(parser.parse("worldCups <> 3", null, null),
                NotEqualsOperation.class, "worldCups", 3L);

        ComparisonOperation<?> gt = castAs(parser.parse("internationalCaps > 1.5e2", null, null), GreaterThanOperation.class);
        assertEquals("internationalCaps", gt.getFieldName());
        assertInstanceOf(Double.class, gt.getValue());
        assertEquals(150.0, (Double) gt.getValue(), 0.000001);

        assertComparison(
                parser.parse("age >= :v", null, values(":v", 38L)),
                GreaterThanEqualsOperation.class, "age", 38L);

        assertComparison(parser.parse("retired < FALSE", null, null),
                LessThanOperation.class, "retired", false);

        assertComparison(parser.parse("nickname <= NULL", null, null),
                LessThanEqualsOperation.class, "nickname", null);
    }

    @Test
    public void testFunctionsLogicalCompositionAliasesAndIndexes() {
        QueryOperation operation = parser.parse(
                "NOT (attribute_exists(#a[0].#b) AND begins_with(email, :p) AND contains(titles, 'World Cup')) "
                        + "OR attribute_type(legendType, S) "
                        + "OR size(teams) >= 2",
                names("#a", "squads", "#b", "captain"),
                values(":p", "messi@")
        );

        OrOperation rootOr = castAs(operation, OrOperation.class);
        assertEquals(3, rootOr.getConditions().size());

        NotOperation not = castAs(rootOr.getConditions().get(0), NotOperation.class);
        AndOperation and = castAs(not.getCondition(), AndOperation.class);
        assertEquals(3, and.getConditions().size());

        ExistsOperation exists = castAs(and.getConditions().get(0), ExistsOperation.class);
        assertEquals("squads[0].captain", exists.getFieldName());
        assertTrue(exists.isExists());

        BeginsWithOperation beginsWith = castAs(and.getConditions().get(1), BeginsWithOperation.class);
        assertEquals("email", beginsWith.getFieldName());
        assertEquals("messi@", beginsWith.getPrefix());

        ContainsOperation contains = castAs(and.getConditions().get(2), ContainsOperation.class);
        assertEquals("titles", contains.getFieldName());
        assertEquals("World Cup", contains.getExpectedValue());

        TypeOperation type = castAs(rootOr.getConditions().get(1), TypeOperation.class);
        assertEquals("legendType", type.getFieldName());
        assertEquals("S", type.getExpectedType());

        SizeOperation size = castAs(rootOr.getConditions().get(2), SizeOperation.class);
        assertEquals("teams", size.getFieldName());
        assertEquals(DynamoDbComparisonType.GREATER_THAN_EQUALS, size.getComparator());
        assertEquals(2L, size.getExpectedValue());
    }

    @Test
    public void testBetweenAndInWithMixedValues() {
        QueryOperation operation = parser.parse(
                "age BETWEEN :low AND 41 AND playerName IN (:s1, 'Maradona', Pele)",
                null,
                values(":low", 38L, ":s1", "Messi")
        );

        AndOperation and = castAs(operation, AndOperation.class);
        assertEquals(2, and.getConditions().size());

        BetweenOperation between = castAs(and.getConditions().get(0), BetweenOperation.class);
        assertEquals("age", between.getFieldName());
        assertEquals(38L, between.getLowerBound());
        assertEquals(41L, between.getUpperBound());

        InOperation<?> in = castAs(and.getConditions().get(1), InOperation.class);
        assertEquals("playerName", in.getFieldName());
        assertEquals(Arrays.asList("Messi", "Maradona", "Pele"), in.getValues());
    }

    @Test
    public void testFallbacksForMissingPlaceholderAliasAndOverflowNumberLiteral() {
        assertComparison(
                parser.parse("#playerId = :missing", Collections.emptyMap(), Collections.emptyMap()),
                EqualsOperation.class,
                "#playerId",
                null
        );

        ComparisonOperation<?> comparison = castAs(parser.parse("allTimeGoals = 9999999999999999999999999999999999999",
                        null, null), EqualsOperation.class);
        assertEquals("allTimeGoals", comparison.getFieldName());
        //Assert the number was parsed as a String as a fallback
        assertEquals("9999999999999999999999999999999999999", comparison.getValue());
    }
}
