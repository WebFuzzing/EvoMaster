package org.evomaster.client.java.controller.cassandra.calculator;

import org.evomaster.client.java.controller.cassandra.model.CassandraRow;
import org.evomaster.client.java.controller.cassandra.model.CqlDurationLiteral;
import org.evomaster.client.java.controller.cassandra.operations.*;
import org.evomaster.client.java.controller.cassandra.parser.CqlDurationLiteralParser;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

import java.net.InetAddress;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.Function;

import static org.evomaster.client.java.controller.cassandra.calculator.CassandraHeuristicsCalculator.*;

/**
 * Computes CQL-heuristics {@link Truthness} for a {@link CqlQueryOperation} against a
 * candidate {@link CassandraRow}, i.e. how close the row comes to satisfying the query's
 * WHERE-clause condition(s). Used to guide the search when EvoMaster generates data intended
 * to make a given Cassandra query return non-empty results.
 */
public class CassandraOperationEvaluator {

    private enum ComparisonType { EQUALS, GT, GTE, LT, LTE }

    /**
     * Each entry attempts to parse a timestamp string under a different format, returning null
     * if that format doesn't match, so {@link #parseTimestampString} can fall through to the next one.
     */
    private static final List<Function<String, Instant>> TIMESTAMP_PARSERS = Arrays.asList(
            CassandraOperationEvaluator::tryParseIsoInstant,
            CassandraOperationEvaluator::tryParseWithTimestampFormatters,
            CassandraOperationEvaluator::tryParseDateWithOffset,
            CassandraOperationEvaluator::tryParseDateOnly
    );

    private static final DateTimeFormatter[] TIMESTAMP_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXX"),
    };

    private static final DateTimeFormatter DATE_WITH_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-ddXX");

    /**
     * Recursively evaluates a {@link CqlQueryOperation} against a row, dispatching to the
     * appropriate comparison logic based on the operation's concrete type (AND, equals,
     * greater/less-than variants, IN, CONTAINS, CONTAINS KEY). Returns {@code FALSE_TRUTHNESS}
     * for operation types that are not recognised/supported.
     */
    public Truthness evaluate(CqlQueryOperation op, CassandraRow candidateRow) {
        if (op instanceof AndOperation) {
            return evaluateAnd((AndOperation) op, candidateRow);
        } else if (op instanceof EqualsOperation<?>) {
            return evaluateEquals((EqualsOperation<?>) op, candidateRow);
        } else if (op instanceof GreaterThanOperation<?>) {
            return evaluateGreaterThan((GreaterThanOperation<?>) op, candidateRow);
        } else if (op instanceof GreaterThanEqualsOperation<?>) {
            return evaluateGreaterThanEquals((GreaterThanEqualsOperation<?>) op, candidateRow);
        } else if (op instanceof LessThanOperation<?>) {
            return evaluateLessThan((LessThanOperation<?>) op, candidateRow);
        } else if (op instanceof LessThanEqualsOperation<?>) {
            return evaluateLessThanEquals((LessThanEqualsOperation<?>) op, candidateRow);
        } else if (op instanceof InOperation) {
            return evaluateIn((InOperation) op, candidateRow);
        } else if (op instanceof ContainsOperation<?>) {
            return evaluateContains((ContainsOperation<?>) op, candidateRow);
        } else if (op instanceof ContainsKeyOperation<?>) {
            return evaluateContainsKey((ContainsKeyOperation<?>) op, candidateRow);
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + op.getClass().getName());
        }
    }

    private Truthness evaluateAnd(AndOperation op, CassandraRow candidateRow) {
        List<Truthness> results = new ArrayList<>();
        for (CqlQueryOperation condition : op.getConditions()) {
            results.add(evaluate(condition, candidateRow));
        }

        return TruthnessUtils.buildAndAggregationTruthness(results.toArray(new Truthness[0]));
    }

    private Truthness evaluateEquals(EqualsOperation<?> op, CassandraRow candidateRow) {
        return evaluateComparison(op, candidateRow, ComparisonType.EQUALS);
    }

    private Truthness evaluateGreaterThan(GreaterThanOperation<?> op, CassandraRow candidateRow) {
        return evaluateComparison(op, candidateRow, ComparisonType.GT);
    }

    private Truthness evaluateGreaterThanEquals(GreaterThanEqualsOperation<?> op, CassandraRow candidateRow) {
        return evaluateComparison(op, candidateRow, ComparisonType.GTE);
    }

    private Truthness evaluateLessThan(LessThanOperation<?> op, CassandraRow candidateRow) {
        return evaluateComparison(op, candidateRow, ComparisonType.LT);
    }

    private Truthness evaluateLessThanEquals(LessThanEqualsOperation<?> op, CassandraRow candidateRow) {
        return evaluateComparison(op, candidateRow, ComparisonType.LTE);
    }

    // In CQL, a stored NULL and an absent/never-set column are the same thing at read time
    // (writing NULL creates a tombstone, identical to never having written the column), so
    // none of the null checks below (here and in evaluateContains, evaluateContainsKey and
    // evaluateComparison) can tell these two cases apart.
    private Truthness evaluateIn(InOperation op, CassandraRow candidateRow) {
        Object rowValue = candidateRow.getValue(op.getColumnName());
        if (rowValue == null) {
            return FALSE_TRUTHNESS;
        }

        return any(rowValue, op.getValues());
    }

    private Truthness evaluateContains(ContainsOperation<?> op, CassandraRow candidateRow) {
        Object rawCol = candidateRow.getValue(op.getColumnName());
        if (rawCol == null) {
            return FALSE_TRUTHNESS;
        }

        return any(op.getValue(), toElementList(rawCol));
    }

    private Truthness evaluateContainsKey(ContainsKeyOperation<?> op, CassandraRow candidateRow) {
        Object rawCol = candidateRow.getValue(op.getColumnName());
        if (rawCol == null) {
            return FALSE_TRUTHNESS;
        } else if (!(rawCol instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("CONTAINS KEY is only supported on map columns, got: " + rawCol.getClass().getName());
        } else {
            List<?> keys = new ArrayList<>(((Map<?, ?>) rawCol).keySet());
            return any(op.getValue(), keys);
        }
    }

    private Truthness evaluateComparison(ComparisonOperation<?> op, CassandraRow candidateRow, ComparisonType type) {
        Object rowValue   = candidateRow.getValue(op.getColumnName());
        Object queryValue = op.getValue();

        if (rowValue == null) {
            return FALSE_TRUTHNESS;
        } else {
            Truthness typeResult = compareByType(rowValue, queryValue, type);
            if (typeResult.isTrue()) {
                return typeResult;
            } else {
                return TruthnessUtils.buildScaledTruthness(C_BETTER, typeResult.getOfTrue());
            }
        }
    }

    private Truthness compareByType(Object rowValue, Object literalValue, ComparisonType comparisonType) {
        if (rowValue instanceof Number && literalValue instanceof Number) {
            return compareNumeric(rowValue, literalValue, comparisonType);
        } else if (rowValue instanceof String || rowValue instanceof InetAddress) {
            return compareString(rowValue, literalValue, comparisonType);
        } else if (rowValue instanceof Boolean) {
            return compareBoolean(rowValue, literalValue, comparisonType);
        } else if (rowValue instanceof UUID) {
            return compareUuid(rowValue, literalValue, comparisonType);
        } else if (isTemporalType(rowValue)) {
            return compareTemporal(rowValue, literalValue, comparisonType);
        } else if (isCqlDuration(rowValue)) {
            return compareDuration((TemporalAmount) rowValue, literalValue, comparisonType);
        } else {
            throw new IllegalArgumentException("Unsupported row value type: " + rowValue.getClass().getName());
        }
    }

    private Truthness compareNumeric(Object rowValue, Object literalValue, ComparisonType comparisonType) {
        double x = ((Number) rowValue).doubleValue();
        double y = ((Number) literalValue).doubleValue();

        return getTruthnessForNumeric(comparisonType, x, y);
    }

    private Truthness compareString(Object rowValue, Object literalValue, ComparisonType comparisonType) {
        if (comparisonType != ComparisonType.EQUALS) {
            throw new IllegalArgumentException("Unsupported operator for string literals: " + comparisonType);
        }

        String a = (rowValue instanceof InetAddress)
                ? ((InetAddress) rowValue).getHostAddress()
                : (String) rowValue;
        String b = (String) literalValue;

        return TruthnessUtils.getStringEqualityTruthness(a, b);
    }

    private Truthness compareBoolean(Object rowValue, Object literalValue, ComparisonType comparisonType) {
        if (comparisonType != ComparisonType.EQUALS) {
            throw new IllegalArgumentException("Unsupported operator for boolean literals: " + comparisonType);
        }

        double x = ((Boolean) rowValue)   ? 1.0 : 0.0;
        double y = ((Boolean) literalValue) ? 1.0 : 0.0;

        return TruthnessUtils.getEqualityTruthness(x, y);
    }

    private Truthness compareUuid(Object rowValue, Object literalValue, ComparisonType comparisonType) {
        if (comparisonType != ComparisonType.EQUALS) {
            throw new IllegalArgumentException("Unsupported operator for UUID literals: " + comparisonType);
        }

        return TruthnessUtils.getEqualityTruthness((UUID) rowValue, (UUID) literalValue);
    }

    private static boolean isTemporalType(Object rowValue) {
        return rowValue instanceof LocalDate
            || rowValue instanceof LocalTime
            || rowValue instanceof Instant;
    }

    private Truthness compareTemporal(Object rowValue, Object literalValue, ComparisonType comparisonType) {
        try {
            double x = (double) toLong(rowValue, rowValue);
            double y = (double) toLong(literalValue, rowValue);

            return getTruthnessForNumeric(comparisonType, x, y);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to compare temporal values: " + e.getMessage(), e);
        }
    }

    private Truthness getTruthnessForNumeric(ComparisonType comparisonType, double x, double y) {
        switch (comparisonType) {
            case EQUALS: return TruthnessUtils.getEqualityTruthness(x, y);
            case GT:     return TruthnessUtils.getLessThanTruthness(y, x);
            case GTE:    return TruthnessUtils.getLessThanTruthness(x, y).invert();
            case LT:     return TruthnessUtils.getLessThanTruthness(x, y);
            case LTE:    return TruthnessUtils.getLessThanTruthness(y, x).invert();
            default:     throw new IllegalArgumentException("Unsupported operator for numeric literals: " + comparisonType);
        }
    }

    /**
     * Converts a temporal value to a comparable epoch-millisecond {@code long}.
     * <p>
     * {@code rowValue} is the value to convert, and it may come either from the row itself
     * or from the query's literal — in the latter case it is often a plain {@link String}
     * (e.g. {@code "2011-02-03"}) that carries no type information of its own. {@code
     * rowValueHint} disambiguates which temporal type ({@link LocalDate}, {@link LocalTime}
     * or {@link Instant}) {@code rowValue} should be interpreted as, since that type is only
     * known from the actual row value's column type. This lets a query literal be parsed and
     * compared using the same temporal interpretation as the row value it's being compared
     * against, even though the literal itself is untyped.
     */
    private static long toLong(Object rowValue, Object rowValueHint) {
        if (rowValueHint instanceof LocalDate) {
            LocalDate dateVal = (rowValue instanceof Long) ? LocalDate.ofEpochDay((Long) rowValue)
                                : (rowValue instanceof String) ? LocalDate.parse((String) rowValue)
                                : (LocalDate) rowValue;

            return dateVal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } else if (rowValueHint instanceof LocalTime) {
            LocalTime timeVal = (rowValue instanceof Long) ? LocalTime.ofNanoOfDay((Long) rowValue)
                                : (rowValue instanceof String) ? LocalTime.parse((String) rowValue)
                                : (LocalTime) rowValue;

            return LocalDateTime.of(LocalDate.of(1970, 1, 1), timeVal).toInstant(ZoneOffset.UTC).toEpochMilli();
        } else if (rowValueHint instanceof Instant) {
            if (rowValue instanceof Long) {
                return (Long) rowValue;
            } else if (rowValue instanceof Instant) {
                return ((Instant) rowValue).toEpochMilli();
            } else if (rowValue instanceof String) {
                return parseTimestampString((String) rowValue).toEpochMilli();
            } else {
                throw new IllegalArgumentException("Unexpected timestamp value type: " + rowValue.getClass());
            }
        } else {
            throw new IllegalArgumentException("Unrecognized temporal type: " + rowValueHint.getClass());
        }
    }

    private static Instant parseTimestampString(String rowValue) {
        for (Function<String, Instant> parser : TIMESTAMP_PARSERS) {
            Instant parsed = parser.apply(rowValue);
            if (parsed != null) {
                return parsed;
            }
        }

        throw new IllegalArgumentException("Cannot parse timestamp string: " + rowValue);
    }

    private static Instant tryParseIsoInstant(String rowValue) {
        try {
            return Instant.parse(rowValue);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Instant tryParseWithTimestampFormatters(String rowValue) {
        for (DateTimeFormatter formatter : TIMESTAMP_FORMATTERS) {
            try {
                return OffsetDateTime.parse(rowValue, formatter).toInstant();
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /**
     * Parses date-only strings with an offset (e.g. {@code "2011-02-03+0000"}), which {@link
     * OffsetDateTime#parse} rejects since they carry no time component. The {@link LocalDate}
     * and {@link ZoneOffset} are extracted from the parsed {@link TemporalAccessor} directly.
     */
    private static Instant tryParseDateWithOffset(String rowValue) {
        try {
            TemporalAccessor accessor = DATE_WITH_OFFSET.parse(rowValue);
            return LocalDate.from(accessor).atStartOfDay(ZoneOffset.from(accessor)).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parses date-only strings without an offset (e.g. {@code "2011-02-03"}), treating them as
     * midnight UTC.
     */
    private static Instant tryParseDateOnly(String rowValue) {
        try {
            return LocalDate.parse(rowValue).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Identifies the driver's {@code CqlDuration} by checking for its distinctive combination of supported temporal
     * units rather than {@code instanceof}, since other JDK types (e.g. {@link Period}) also implement {@link TemporalAmount}.
     */
    private static boolean isCqlDuration(Object rowValue) {
        if (!(rowValue instanceof TemporalAmount)) {
            return false;
        } else {
            List<TemporalUnit> units = ((TemporalAmount) rowValue).getUnits();
            return units.contains(ChronoUnit.MONTHS)
                    && units.contains(ChronoUnit.DAYS)
                    && units.contains(ChronoUnit.NANOS);
        }
    }

    private Truthness compareDuration(TemporalAmount rowValue, Object literalValue, ComparisonType comparisonType) {
        if (comparisonType != ComparisonType.EQUALS) {
            throw new IllegalArgumentException("Unsupported operator for duration literals: " + comparisonType);
        }

        long months = rowValue.get(ChronoUnit.MONTHS);
        long days = rowValue.get(ChronoUnit.DAYS);
        long nanos = rowValue.get(ChronoUnit.NANOS);

        CqlDurationLiteral literalDurationValue = CqlDurationLiteralParser.parse((String) literalValue);

        Truthness monthsTruthness = TruthnessUtils.getEqualityTruthness(months, literalDurationValue.months);
        Truthness daysTruthness = TruthnessUtils.getEqualityTruthness(days, literalDurationValue.days);
        Truthness nanosTruthness = TruthnessUtils.getEqualityTruthness(nanos, literalDurationValue.nanos);

        return TruthnessUtils.buildAndAggregationTruthness(
                monthsTruthness,
                daysTruthness,
                nanosTruthness
        );
    }

    private Truthness any(Object value, List<?> candidates) {
        if (candidates.isEmpty()) {
            return FALSE_TRUTHNESS;
        } else {
            Truthness[] truthnesses = candidates.stream()
                    .map(candidate -> evaluateEquals(value, candidate))
                    .toArray(Truthness[]::new);

            return TruthnessUtils.buildOrAggregationTruthness(truthnesses);
        }
    }

    private Truthness evaluateEquals(Object a, Object b) {
        Truthness unscaledTruthness = compareByType(a, b, ComparisonType.EQUALS);
        if (unscaledTruthness.isTrue()) {
            return unscaledTruthness;
        }

        return TruthnessUtils.buildScaledTruthness(C_BETTER, unscaledTruthness.getOfTrue());
    }

    private static List<?> toElementList(Object collection) {
        if (collection instanceof List<?>) {
            return (List<?>) collection;
        } else if (collection instanceof Set<?>) {
            return new ArrayList<>((Set<?>) collection);
        } else if (collection instanceof Map<?, ?>) {
            return new ArrayList<>(((Map<?, ?>) collection).values());
        } else {
            return Collections.emptyList();
        }
    }

}