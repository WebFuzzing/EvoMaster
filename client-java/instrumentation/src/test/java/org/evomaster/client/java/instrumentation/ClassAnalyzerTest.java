package org.evomaster.client.java.instrumentation;

import com.foo.somedifferentpackage.examples.entity.EntityA;
import com.foo.somedifferentpackage.examples.entity.EntityX;
import com.foo.somedifferentpackage.examples.entity.EntityY;
import com.foo.somedifferentpackage.examples.entity.EntityZ;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ClassAnalyzerTest {

    @BeforeEach
    void setUp() {
        UnitsInfoRecorder.reset();
    }

    @Test
    void testNullable() {
        ClassAnalyzer.doAnalyze(Arrays.asList(
                EntityX.class.getName(),
                EntityY.class.getName() // Mapped to BAR
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> x_constraints = jpa.stream().filter(j -> j.getTableName().equals("entity_x")).collect(Collectors.toList());
        List<JpaConstraint> y_constraints = jpa.stream().filter(j -> j.getTableName().equals("bar")).collect(Collectors.toList());

        assertEquals(2, x_constraints.size());
        assertEquals(4, y_constraints.size());

        assertTrue(x_constraints.stream().anyMatch(j -> j.getColumnName().equals("y") && !j.getNullable()));
        assertTrue(x_constraints.stream().anyMatch(j -> j.getColumnName().equals("k") && !j.getNullable()));

        assertTrue(y_constraints.stream().anyMatch(j -> j.getColumnName().equals("x") && !j.getNullable()));
        assertTrue(y_constraints.stream().anyMatch(j -> j.getColumnName().equals("foo") && !j.getNullable()));
        assertTrue(y_constraints.stream().anyMatch(j -> j.getColumnName().equals("hello") && !j.getNullable()));
        assertTrue(y_constraints.stream().anyMatch(j -> j.getColumnName().equals("k") && !j.getNullable()));
    }

    @Test
    void testEnum() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityZ.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> z = jpa.stream().filter(j -> j.getTableName().equals("entity_z")).collect(Collectors.toList());

        assertEquals(1, z.size());

        JpaConstraint e = z.get(0);

        assertEquals("foo", e.getColumnName());
        assertNotNull(e.getEnumValuesAsStrings());
        assertEquals(3, e.getEnumValuesAsStrings().size());
        assertTrue(e.getEnumValuesAsStrings().contains("A"));
        assertTrue(e.getEnumValuesAsStrings().contains("B"));
        assertTrue(e.getEnumValuesAsStrings().contains("C"));
    }

    @Test
    void testMinAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());

        List<JpaConstraint> min_value_column = entity_a.stream().filter(c -> c.getColumnName().equals("min_value_column")).collect(Collectors.toList());
        assertEquals(1, min_value_column.size());
        JpaConstraint min_value_constraint = min_value_column.get(0);
        assertEquals(-1L, min_value_constraint.getMinValue());
    }

    @Test
    void testMaxAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> max_value_column = entity_a.stream().filter(c -> c.getColumnName().equals("max_value_column")).collect(Collectors.toList());

        assertEquals(1, max_value_column.size());
        JpaConstraint max_value_constraint = max_value_column.get(0);
        assertEquals(200L, max_value_constraint.getMaxValue());
    }

    @Test
    void testNotBlankAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> not_blank_column = entity_a.stream().filter(c -> c.getColumnName().equals("not_blank_column")).collect(Collectors.toList());

        assertEquals(1, not_blank_column.size());
        JpaConstraint not_blank_constraint = not_blank_column.get(0);
        assertTrue(not_blank_constraint.getNotBlank());
    }

    @Test
    void testEMailAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> email_column = entity_a.stream().filter(c -> c.getColumnName().equals("email_column")).collect(Collectors.toList());

        assertEquals(1, email_column.size());
        JpaConstraint email_constraint = email_column.get(0);
        assertTrue(email_constraint.getIsEmail());
    }


    @Test
    void testPositiveAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> positive_column = entity_a.stream().filter(c -> c.getColumnName().equals("positive_column")).collect(Collectors.toList());

        assertEquals(1, positive_column.size());
        JpaConstraint positive_constraint = positive_column.get(0);
        assertTrue(positive_constraint.getIsPositive());
    }

    @Test
    void testPositiveOrZeroAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> positive_or_zero_column = entity_a.stream().filter(c -> c.getColumnName().equals("positive_or_zero_column")).collect(Collectors.toList());

        assertEquals(1, positive_or_zero_column.size());
        JpaConstraint positive_or_zero_constraint = positive_or_zero_column.get(0);
        assertTrue(positive_or_zero_constraint.getIsPositiveOrZero());
    }

    @Test
    void testNegativeAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> negative_column = entity_a.stream().filter(c -> c.getColumnName().equals("negative_column")).collect(Collectors.toList());

        assertEquals(1, negative_column.size());
        JpaConstraint negative_constraint = negative_column.get(0);
        assertTrue(negative_constraint.getIsNegative());
    }

    @Test
    void testNegativeOrZeroAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> negative_or_zero_column = entity_a.stream().filter(c -> c.getColumnName().equals("negative_or_zero_column")).collect(Collectors.toList());

        assertEquals(1, negative_or_zero_column.size());
        JpaConstraint negative_or_zero_constraint = negative_or_zero_column.get(0);
        assertTrue(negative_or_zero_constraint.getIsNegativeOrZero());
    }

    @Test
    void testPastAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> past_column = entity_a.stream().filter(c -> c.getColumnName().equals("past_column")).collect(Collectors.toList());

        assertEquals(1, past_column.size());
        JpaConstraint past_constraint = past_column.get(0);
        assertTrue(past_constraint.getIsPast());
    }

    @Test
    void testPastOrPresentAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> past_or_present_column = entity_a.stream().filter(c -> c.getColumnName().equals("past_or_present_column")).collect(Collectors.toList());

        assertEquals(1, past_or_present_column.size());
        JpaConstraint past_or_present_constraint = past_or_present_column.get(0);
        assertTrue(past_or_present_constraint.getIsPastOrPresent());
    }

    @Test
    void testFutureOrPresentAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> future_or_present_column = entity_a.stream().filter(c -> c.getColumnName().equals("future_or_present_column")).collect(Collectors.toList());

        assertEquals(1, future_or_present_column.size());
        JpaConstraint future_or_present_constraint = future_or_present_column.get(0);
        assertTrue(future_or_present_constraint.getIsFutureOrPresent());
    }

    @Test
    void testFutureAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> future_column = entity_a.stream().filter(c -> c.getColumnName().equals("future_column")).collect(Collectors.toList());

        assertEquals(1, future_column.size());
        JpaConstraint future_constraint = future_column.get(0);
        assertTrue(future_constraint.getIsFuture());
    }

    @Test
    void testNullAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());
        List<JpaConstraint> null_column = entity_a.stream().filter(c -> c.getColumnName().equals("null_column")).collect(Collectors.toList());

        assertEquals(1, null_column.size());
        JpaConstraint null_column_constraint = null_column.get(0);
        assertTrue(null_column_constraint.getIsAlwaysNull());
    }

    @Test
    void testDecimalMinAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());

        List<JpaConstraint> decimal_min_column = entity_a.stream().filter(c -> c.getColumnName().equals("decimal_min_column")).collect(Collectors.toList());
        assertEquals(1, decimal_min_column.size());
        JpaConstraint decimal_min_column_constraint = decimal_min_column.get(0);
        assertEquals("-1.0", decimal_min_column_constraint.getDecimalMinValue());
    }

    @Test
    void testDecimalMaxAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());

        List<JpaConstraint> decimal_max_column = entity_a.stream().filter(c -> c.getColumnName().equals("decimal_max_column")).collect(Collectors.toList());
        assertEquals(1, decimal_max_column.size());
        JpaConstraint decimal_max_column_constraint = decimal_max_column.get(0);
        assertEquals("42.0", decimal_max_column_constraint.getDecimalMaxValue());
    }

    @Test
    void testPatternAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());

        List<JpaConstraint> pattern_column = entity_a.stream().filter(c -> c.getColumnName().equals("pattern_column")).collect(Collectors.toList());
        assertEquals(1, pattern_column.size());
        JpaConstraint pattern_column_constraint = pattern_column.get(0);
        assertEquals("[0-9]+", pattern_column_constraint.getPatternRegExp());
    }

    @Test
    void testSizeAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());

        List<JpaConstraint> only_size_min_column = entity_a.stream().filter(c -> c.getColumnName().equals("only_size_min_column")).collect(Collectors.toList());
        assertEquals(1, only_size_min_column.size());
        JpaConstraint only_size_min_column_constraint = only_size_min_column.get(0);
        assertEquals(3, only_size_min_column_constraint.getSizeMin());
        assertEquals(Integer.MAX_VALUE, only_size_min_column_constraint.getSizeMax());

        List<JpaConstraint> only_size_max_column = entity_a.stream().filter(c -> c.getColumnName().equals("only_size_max_column")).collect(Collectors.toList());
        assertEquals(1, only_size_max_column.size());
        JpaConstraint only_size_max_column_constraint = only_size_max_column.get(0);
        assertEquals(0, only_size_max_column_constraint.getSizeMin());
        assertEquals(10, only_size_max_column_constraint.getSizeMax());

        List<JpaConstraint> no_size_min_max_column = entity_a.stream().filter(c -> c.getColumnName().equals("no_size_min_max_column")).collect(Collectors.toList());
        assertEquals(1, no_size_min_max_column.size());
        JpaConstraint no_size_min_max_column_constraint = no_size_min_max_column.get(0);
        assertEquals(0, no_size_min_max_column_constraint.getSizeMin());
        assertEquals(Integer.MAX_VALUE, no_size_min_max_column_constraint.getSizeMax());

        List<JpaConstraint> negative_size_min_column = entity_a.stream().filter(c -> c.getColumnName().equals("negative_size_min_column")).collect(Collectors.toList());
        assertEquals(1, negative_size_min_column.size());
        JpaConstraint negative_size_min_column_constraint = negative_size_min_column.get(0);
        assertEquals(-1, negative_size_min_column_constraint.getSizeMin());
        assertEquals(Integer.MAX_VALUE, negative_size_min_column_constraint.getSizeMax());

        List<JpaConstraint> negative_size_max_column = entity_a.stream().filter(c -> c.getColumnName().equals("negative_size_max_column")).collect(Collectors.toList());
        assertEquals(1, negative_size_max_column.size());
        JpaConstraint negative_size_max_column_constraint = negative_size_max_column.get(0);
        assertEquals(0, negative_size_max_column_constraint.getSizeMin());
        assertEquals(-2, negative_size_max_column_constraint.getSizeMax());
    }

    @Test
    void testFractionAnnotation() {
        ClassAnalyzer.doAnalyze(Collections.singletonList(
                EntityA.class.getName()
        ));

        List<JpaConstraint> jpa = UnitsInfoRecorder.getInstance().getJpaConstraints();
        assertFalse(jpa.isEmpty());

        List<JpaConstraint> entity_a = jpa.stream().filter(j -> j.getTableName().equals("entity_a")).collect(Collectors.toList());

        List<JpaConstraint> digits_column = entity_a.stream().filter(c -> c.getColumnName().equals("digits_column")).collect(Collectors.toList());
        assertEquals(1, digits_column.size());
        JpaConstraint digits_column_constraint = digits_column.get(0);
        assertEquals(3, digits_column_constraint.getDigitsInteger());
        assertEquals(7, digits_column_constraint.getDigitsFraction());
    }
}