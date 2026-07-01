package org.evomaster.client.java.controller.cassandra;

import org.evomaster.client.java.controller.cassandra.operations.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static org.evomaster.client.java.controller.cassandra.CassandraHeuristicsCalculator.*;

public class CassandraOperationEvaluator {

    private static final Object MISSING = new Object();

    private enum ComparisonType { EQUALS, GT, GTE, LT, LTE }

    public Truthness evaluate(CqlQueryOperation op, Map<String, Object> row) {
        if (op instanceof AndOperation)
            return evaluateAnd((AndOperation) op, row);
        if (op instanceof EqualsOperation<?>)
            return evaluateEquals((EqualsOperation<?>) op, row);
        if (op instanceof GreaterThanOperation<?>)
            return evaluateGreaterThan((GreaterThanOperation<?>) op, row);
        if (op instanceof GreaterThanEqualsOperation<?>)
            return evaluateGreaterThanEquals((GreaterThanEqualsOperation<?>) op, row);
        if (op instanceof LessThanOperation<?>)
            return evaluateLessThan((LessThanOperation<?>) op, row);
        if (op instanceof LessThanEqualsOperation<?>)
            return evaluateLessThanEquals((LessThanEqualsOperation<?>) op, row);
        if (op instanceof InOperation)
            return evaluateIn((InOperation) op, row);
        if (op instanceof ContainsOperation<?>)
            return evaluateContains((ContainsOperation<?>) op, row);
        if (op instanceof ContainsKeyOperation<?>)
            return evaluateContainsKey((ContainsKeyOperation<?>) op, row);
        return FALSE_TRUTHNESS;
    }

    private Truthness evaluateAnd(AndOperation op, Map<String, Object> row) {
        List<Truthness> results = new ArrayList<>();
        for (CqlQueryOperation condition : op.getConditions()) {
            results.add(evaluate(condition, row));
        }
        return TruthnessUtils.buildAndAggregationTruthness(results.toArray(new Truthness[0]));
    }

    private Truthness evaluateEquals(EqualsOperation<?> op, Map<String, Object> row) {
        return evaluateComparison(op, row, ComparisonType.EQUALS);
    }

    private Truthness evaluateGreaterThan(GreaterThanOperation<?> op, Map<String, Object> row) {
        return evaluateComparison(op, row, ComparisonType.GT);
    }

    private Truthness evaluateGreaterThanEquals(GreaterThanEqualsOperation<?> op, Map<String, Object> row) {
        return evaluateComparison(op, row, ComparisonType.GTE);
    }

    private Truthness evaluateLessThan(LessThanOperation<?> op, Map<String, Object> row) {
        return evaluateComparison(op, row, ComparisonType.LT);
    }

    private Truthness evaluateLessThanEquals(LessThanEqualsOperation<?> op, Map<String, Object> row) {
        return evaluateComparison(op, row, ComparisonType.LTE);
    }

    private Truthness evaluateIn(InOperation op, Map<String, Object> row) {
        Object rowValue = getRowValue(row, op.getColumnName());
        if (rowValue == MISSING) rowValue = null;
        return any(rowValue, op.getValues());
    }

    private Truthness evaluateContains(ContainsOperation<?> op, Map<String, Object> row) {
        Object rawCol = getRowValue(row, op.getColumnName());
        if (rawCol == MISSING || rawCol == null) return FALSE_TRUTHNESS;
        return any(op.getValue(), toElementList(rawCol));
    }

    private Truthness evaluateContainsKey(ContainsKeyOperation<?> op, Map<String, Object> row) {
        Object rawCol = getRowValue(row, op.getColumnName());
        if (rawCol == MISSING || rawCol == null) return FALSE_TRUTHNESS;
        if (!(rawCol instanceof Map<?, ?>)) return FALSE_TRUTHNESS;
        List<?> keys = new ArrayList<>(((Map<?, ?>) rawCol).keySet());
        return any(op.getValue(), keys);
    }

    private Truthness evaluateComparison(ComparisonOperation<?> op, Map<String, Object> row, ComparisonType type) {
        Object rowValue   = getRowValue(row, op.getColumnName());
        Object queryValue = op.getValue();

        if (rowValue == MISSING) rowValue = null;

        if (rowValue == null && queryValue == null) return FALSE_TRUTHNESS;
        if (rowValue == null || queryValue == null)  return FALSE_TRUTHNESS_BETTER;

        Truthness typeResult = compareByType(rowValue, queryValue, type);
        if (typeResult.isTrue()) {
            return typeResult;
        }
        return TruthnessUtils.buildScaledTruthness(C_BETTER, typeResult.getOfTrue());
    }

    private Truthness compareByType(Object rowVal, Object queryVal, ComparisonType type) {
        if (rowVal instanceof Number && queryVal instanceof Number)
            return compareNumeric(rowVal, queryVal, type);
        if (rowVal instanceof String || rowVal instanceof InetAddress)
            return compareString(rowVal, queryVal, type);
        if (rowVal instanceof Boolean)
            return compareBoolean(rowVal, queryVal, type);
        if (rowVal instanceof UUID)
            return compareUuid(rowVal, queryVal, type);
        if (isTemporalType(rowVal))
            return compareTemporal(rowVal, queryVal, type);
        if (isCqlDuration(rowVal))
            return compareDuration(rowVal, queryVal, type);

        SimpleLogger.uniqueWarn("CassandraHeuristicsCalculator: unsupported type "
                + rowVal.getClass().getName() + " — returning FALSE_TRUTHNESS");
        return FALSE_TRUTHNESS;
    }

    private Truthness compareNumeric(Object rowVal, Object queryVal, ComparisonType type) {
        double x = ((Number) rowVal).doubleValue();
        double y = ((Number) queryVal).doubleValue();
        return getTruthness(type, x, y);
    }

    private Truthness compareString(Object rowVal, Object queryVal, ComparisonType type) {
        if (type != ComparisonType.EQUALS) return FALSE_TRUTHNESS;
        String a = (rowVal instanceof InetAddress)
                ? ((InetAddress) rowVal).getHostAddress()
                : (String) rowVal;
        String b = (String) queryVal;
        return TruthnessUtils.getStringEqualityTruthness(a, b);
    }

    private Truthness compareBoolean(Object rowVal, Object queryVal, ComparisonType type) {
        if (type != ComparisonType.EQUALS) return FALSE_TRUTHNESS;
        double x = ((Boolean) rowVal)   ? 1.0 : 0.0;
        double y = ((Boolean) queryVal) ? 1.0 : 0.0;
        return TruthnessUtils.getEqualityTruthness(x, y);
    }

    private Truthness compareUuid(Object rowVal, Object queryVal, ComparisonType type) {
        if (type != ComparisonType.EQUALS) return FALSE_TRUTHNESS;
        return TruthnessUtils.getEqualityTruthness((UUID) rowVal, (UUID) queryVal);
    }

    private static boolean isTemporalType(Object value) {
        return value instanceof LocalDate
            || value instanceof LocalTime
            || value instanceof Instant;
    }

    private Truthness compareTemporal(Object rowVal, Object queryVal, ComparisonType type) {
        try {
            double x = (double) toLong(rowVal, rowVal);
            double y = (double) toLong(queryVal, rowVal);
            return getTruthness(type, x, y);
        } catch (Exception e) {
            return FALSE_TRUTHNESS;
        }
    }

    private Truthness getTruthness(ComparisonType type, double x, double y) {
        switch (type) {
            case EQUALS: return TruthnessUtils.getEqualityTruthness(x, y);
            case GT:     return TruthnessUtils.getLessThanTruthness(y, x);
            case GTE:    return TruthnessUtils.getLessThanTruthness(x, y).invert();
            case LT:     return TruthnessUtils.getLessThanTruthness(x, y);
            case LTE:    return TruthnessUtils.getLessThanTruthness(y, x).invert();
            default:     return FALSE_TRUTHNESS;
        }
    }

    private static long toLong(Object value, Object rowValueHint) {
        if (rowValueHint instanceof LocalDate) {
            LocalDate dateVal = (value instanceof Long)   ? LocalDate.ofEpochDay((Long) value)
                        : (value instanceof String) ? LocalDate.parse((String) value)
                        : (LocalDate) value;
            return dateVal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        if (rowValueHint instanceof LocalTime) {
            LocalTime timeVal = (value instanceof Long)   ? LocalTime.ofNanoOfDay((Long) value)
                        : (value instanceof String) ? LocalTime.parse((String) value)
                        : (LocalTime) value;
            return LocalDateTime.of(LocalDate.of(1970, 1, 1), timeVal).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        if (rowValueHint instanceof Instant) {
            if (value instanceof Long)    return (Long) value;
            if (value instanceof Instant) return ((Instant) value).toEpochMilli();
            if (value instanceof String)  return parseTimestampString((String) value).toEpochMilli();
            throw new IllegalArgumentException("Unexpected timestamp value type: " + value.getClass());
        }
        throw new IllegalArgumentException("Unrecognized temporal type: " + rowValueHint.getClass());
    }

    private static final DateTimeFormatter[] TIMESTAMP_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXX"),
    };

    private static final DateTimeFormatter DATE_WITH_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-ddXX");

    private static Instant parseTimestampString(String s) {
        try { return Instant.parse(s); } catch (DateTimeParseException ignored) {}
        for (DateTimeFormatter formatter : TIMESTAMP_FORMATTERS) {
            try { return OffsetDateTime.parse(s, formatter).toInstant(); } catch (DateTimeParseException ignored) {}
        }
        // date-only with offset ("2011-02-03+0000"): OffsetDateTime.parse fails without a time
        // component, so extract LocalDate and ZoneOffset from the TemporalAccessor directly.
        try {
            TemporalAccessor accessor = DATE_WITH_OFFSET.parse(s);
            return LocalDate.from(accessor).atStartOfDay(ZoneOffset.from(accessor)).toInstant();
        } catch (DateTimeParseException ignored) {}
        // date-only without offset ("2011-02-03"): treat as midnight UTC
        try { return LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant(); } catch (DateTimeParseException ignored) {}
        throw new IllegalArgumentException("Cannot parse timestamp string: " + s);
    }

    private static boolean isCqlDuration(Object value) {
        return value.getClass().getName()
                .equals("com.datastax.oss.driver.api.core.data.CqlDuration");
    }

    private static int getDurationMonths(Object durationVal) throws Exception {
        return (int) durationVal.getClass().getMethod("getMonths").invoke(durationVal);
    }

    private static int getDurationDays(Object durationVal) throws Exception {
        return (int) durationVal.getClass().getMethod("getDays").invoke(durationVal);
    }

    private static long getDurationNanos(Object durationVal) throws Exception {
        return (long) durationVal.getClass().getMethod("getNanoseconds").invoke(durationVal);
    }

    private Truthness compareDuration(Object rowVal, Object queryVal, ComparisonType type) {
        if (type != ComparisonType.EQUALS) return FALSE_TRUTHNESS;
        try {
            int  months = getDurationMonths(rowVal);
            int  days = getDurationDays(rowVal);
            long nanos = getDurationNanos(rowVal);

            CqlDurationLiteral q = CqlDurationLiteral.parse((String) queryVal);

            return TruthnessUtils.buildAndAggregationTruthness(
                    TruthnessUtils.getEqualityTruthness((long) months, (long) q.months),
                    TruthnessUtils.getEqualityTruthness((long) days, (long) q.days),
                    TruthnessUtils.getEqualityTruthness(nanos, q.nanos)
            );
        } catch (Exception e) {
            return FALSE_TRUTHNESS;
        }
    }

    private Truthness any(Object value, List<?> candidates) {
        if (candidates.isEmpty()) return FALSE_TRUTHNESS;
        Truthness[] truthnesses = candidates.stream()
                .map(candidate -> evaluateEquals(value, candidate))
                .toArray(Truthness[]::new);
        return TruthnessUtils.buildOrAggregationTruthness(truthnesses);
    }

    private Truthness evaluateEquals(Object a, Object b) {
        if (a == null && b == null) return FALSE_TRUTHNESS;
        if (a == null || b == null) return FALSE_TRUTHNESS_BETTER;
        Truthness raw = compareByType(a, b, ComparisonType.EQUALS);
        if (raw.isTrue()) return raw;
        return TruthnessUtils.buildScaledTruthness(C_BETTER, raw.getOfTrue());
    }

    private static List<?> toElementList(Object collection) {
        if (collection instanceof List<?>)  return (List<?>) collection;
        if (collection instanceof Set<?>)   return new ArrayList<>((Set<?>) collection);
        if (collection instanceof Map<?, ?>) return new ArrayList<>(((Map<?, ?>) collection).values());
        return Collections.emptyList();
    }

    private static String normalizeColumnName(String name) {
        if (name == null) return null;
        if (name.startsWith("\"") && name.endsWith("\"")) {
            return name.substring(1, name.length() - 1).toLowerCase();
        }
        return name.toLowerCase();
    }

    private Object getRowValue(Map<String, Object> row, String rawColumnName) {
        String key = normalizeColumnName(rawColumnName);
        if (!row.containsKey(key)) return MISSING;
        return row.get(key);
    }
}