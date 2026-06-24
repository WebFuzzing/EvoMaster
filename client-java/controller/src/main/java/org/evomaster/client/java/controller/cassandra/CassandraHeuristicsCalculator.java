package org.evomaster.client.java.controller.cassandra;

import org.evomaster.client.java.controller.cassandra.operations.*;
import org.evomaster.client.java.controller.cassandra.parser.CqlParserUtils;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class CassandraHeuristicsCalculator {

    private static final double C        = DistanceHelper.H_NOT_NULL;
    private static final double C_BETTER = 0.15;

    private static final Truthness TRUE_TRUTHNESS         = new Truthness(1.0, C);
    private static final Truthness FALSE_TRUTHNESS        = new Truthness(C, 1.0);
    private static final Truthness FALSE_TRUTHNESS_BETTER = new Truthness(C_BETTER, 1.0);

    private static final Object MISSING = new Object();

    private enum ComparisonType { EQUALS, GT, GTE, LT, LTE }

    public double computeDistance(String cqlQuery, Iterable<Map<String, Object>> allRows) {
        return 1.0 - computeHQuery(cqlQuery, allRows).getOfTrue();
    }

    private Truthness computeHQuery(String cqlQuery, Iterable<Map<String, Object>> allRows) {
        if (!CqlParserUtils.canParseCqlCommand(cqlQuery)) {
            return FALSE_TRUTHNESS;
        }

        List<Map<String, Object>> rows = toList(allRows);

        org.evomaster.client.java.controller.cassandra.parser.CqlParser.RootContext root =
                CqlParserUtils.parseCqlCommand(cqlQuery);
        CqlQueryOperation whereOp = CqlParserUtils.getWhereOperation(root);

        Truthness hRowSet = computeHRowSet(rows);

        if (whereOp == null) {
            return hRowSet;
        }

        Truthness hCondition = computeHCondition(whereOp, rows);
        return TruthnessUtils.buildAndAggregationTruthness(hRowSet, hCondition);
    }

    private Truthness computeHRowSet(List<Map<String, Object>> rows) {
        return TruthnessUtils.getTruthnessToEmpty(rows.size()).invert();
    }

    private Truthness computeHCondition(CqlQueryOperation condition,
                                        List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return FALSE_TRUTHNESS;
        }

        double maxOfTrue = 0.0;
        for (Map<String, Object> row : rows) {
            double ofTrue = calculateDistance(condition, row).getOfTrue();
            if (ofTrue >= 1.0) {
                return TRUE_TRUTHNESS;
            }
            if (ofTrue > maxOfTrue) {
                maxOfTrue = ofTrue;
            }
        }

        return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
    }

    private Truthness calculateDistance(CqlQueryOperation op, Map<String, Object> row) {
        if (op instanceof AndOperation)
            return calculateDistanceForAnd((AndOperation) op, row);
        if (op instanceof EqualsOperation<?>)
            return calculateDistanceForComparison((ComparisonOperation<?>) op, row, ComparisonType.EQUALS);
        if (op instanceof GreaterThanOperation<?>)
            return calculateDistanceForComparison((ComparisonOperation<?>) op, row, ComparisonType.GT);
        if (op instanceof GreaterThanEqualsOperation<?>)
            return calculateDistanceForComparison((ComparisonOperation<?>) op, row, ComparisonType.GTE);
        if (op instanceof LessThanOperation<?>)
            return calculateDistanceForComparison((ComparisonOperation<?>) op, row, ComparisonType.LT);
        if (op instanceof LessThanEqualsOperation<?>)
            return calculateDistanceForComparison((ComparisonOperation<?>) op, row, ComparisonType.LTE);
        if (op instanceof InOperation)
            return calculateDistanceForIn((InOperation) op, row);
        if (op instanceof ContainsOperation<?>)
            return calculateDistanceForContains((ContainsOperation<?>) op, row);
        if (op instanceof ContainsKeyOperation<?>)
            return calculateDistanceForContainsKey((ContainsKeyOperation<?>) op, row);

        return FALSE_TRUTHNESS;
    }

    private Truthness calculateDistanceForAnd(AndOperation op, Map<String, Object> row) {
        List<Truthness> results = new ArrayList<>();
        for (CqlQueryOperation condition : op.getConditions()) {
            results.add(calculateDistance(condition, row));
        }
        return TruthnessUtils.buildAndAggregationTruthness(results.toArray(new Truthness[0]));
    }

    private Truthness calculateDistanceForComparison(
            ComparisonOperation<?> op,
            Map<String, Object> row,
            ComparisonType type) {

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
        switch (type) {
            case EQUALS: return TruthnessUtils.getEqualityTruthness(x, y);
            case GT:     return TruthnessUtils.getLessThanTruthness(y, x);
            case GTE:    return TruthnessUtils.getLessThanTruthness(x, y).invert();
            case LT:     return TruthnessUtils.getLessThanTruthness(x, y);
            case LTE:    return TruthnessUtils.getLessThanTruthness(y, x).invert();
            default:     return FALSE_TRUTHNESS;
        }
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

    private static boolean isTemporalType(Object v) {
        return v instanceof LocalDate
            || v instanceof LocalTime
            || v instanceof Instant;
    }

    private Truthness compareTemporal(Object rowVal, Object queryVal, ComparisonType type) {
        try {
            double x = (double) toLong(rowVal, rowVal);
            double y = (double) toLong(queryVal, rowVal);
            switch (type) {
                case EQUALS: return TruthnessUtils.getEqualityTruthness(x, y);
                case GT:     return TruthnessUtils.getLessThanTruthness(y, x);
                case GTE:    return TruthnessUtils.getLessThanTruthness(x, y).invert();
                case LT:     return TruthnessUtils.getLessThanTruthness(x, y);
                case LTE:    return TruthnessUtils.getLessThanTruthness(y, x).invert();
                default:     return FALSE_TRUTHNESS;
            }
        } catch (Exception e) {
            return FALSE_TRUTHNESS;
        }
    }

    private static long toLong(Object value, Object rowValueHint) {
        if (rowValueHint instanceof LocalDate) {
            if (value instanceof String) return LocalDate.parse((String) value).toEpochDay();
            return ((LocalDate) value).toEpochDay();
        }
        if (rowValueHint instanceof LocalTime) {
            if (value instanceof String) return LocalTime.parse((String) value).toNanoOfDay();
            return ((LocalTime) value).toNanoOfDay();
        }
        if (rowValueHint instanceof Instant) {
            if (value instanceof String) return Instant.parse((String) value).toEpochMilli();
            return ((Instant) value).toEpochMilli();
        }
        throw new IllegalArgumentException("Unrecognized temporal type: " + rowValueHint.getClass());
    }

    private static boolean isCqlDuration(Object v) {
        return v.getClass().getName()
                .equals("com.datastax.oss.driver.api.core.data.CqlDuration");
    }

    private static int getDurationMonths(Object d) throws Exception {
        return (int) d.getClass().getMethod("getMonths").invoke(d);
    }

    private static int getDurationDays(Object d) throws Exception {
        return (int) d.getClass().getMethod("getDays").invoke(d);
    }

    private static long getDurationNanos(Object d) throws Exception {
        return (long) d.getClass().getMethod("getNanoseconds").invoke(d);
    }

    private Truthness compareDuration(Object rowVal, Object queryVal, ComparisonType type) {
        if (type != ComparisonType.EQUALS) return FALSE_TRUTHNESS;
        try {
            int  rm = getDurationMonths(rowVal);
            int  rd = getDurationDays(rowVal);
            long rn = getDurationNanos(rowVal);

            CqlDurationLiteral q = CqlDurationLiteral.parse((String) queryVal);

            return TruthnessUtils.buildAndAggregationTruthness(
                    TruthnessUtils.getEqualityTruthness((long) rm, (long) q.months),
                    TruthnessUtils.getEqualityTruthness((long) rd, (long) q.days),
                    TruthnessUtils.getEqualityTruthness(rn, q.nanos)
            );
        } catch (Exception e) {
            return FALSE_TRUTHNESS;
        }
    }

    private Truthness calculateDistanceForIn(InOperation op, Map<String, Object> row) {
        Object rowValue = getRowValue(row, op.getColumnName());
        if (rowValue == MISSING) rowValue = null;
        return computeEqualsAny(rowValue, op.getValues());
    }

    private Truthness calculateDistanceForContains(ContainsOperation<?> op,
                                                   Map<String, Object> row) {
        Object rawCol = getRowValue(row, op.getColumnName());
        if (rawCol == MISSING || rawCol == null) return FALSE_TRUTHNESS;
        // Elements are the authoritative side — pass them first so compareByType dispatches on their type.
        return computeEqualsAnyElements(toElementList(rawCol), op.getValue());
    }

    private Truthness calculateDistanceForContainsKey(ContainsKeyOperation<?> op,
                                                      Map<String, Object> row) {
        Object rawCol = getRowValue(row, op.getColumnName());
        if (rawCol == MISSING || rawCol == null) return FALSE_TRUTHNESS;
        if (!(rawCol instanceof Map<?, ?>)) return FALSE_TRUTHNESS;
        List<?> keys = new ArrayList<>(((Map<?, ?>) rawCol).keySet());
        // Keys are the authoritative side — pass them first so compareByType dispatches on their type.
        return computeEqualsAnyElements(keys, op.getValue());
    }

    /**
     * Used by IN: value is the row column (authoritative type), candidates are the query IN-list values.
     * compareByType dispatches on value.
     */
    private Truthness computeEqualsAny(Object value, List<?> candidates) {
        if (candidates.isEmpty()) return FALSE_TRUTHNESS;

        double maxOfTrue = 0.0;
        for (Object candidate : candidates) {
            Truthness t = evaluateEquals(value, candidate);
            if (t.isTrue()) return TRUE_TRUTHNESS;
            if (t.getOfTrue() > maxOfTrue) maxOfTrue = t.getOfTrue();
        }
        return new Truthness(maxOfTrue, 1.0);
    }

    /**
     * Used by CONTAINS / CONTAINS KEY: elements are the collection elements (authoritative type),
     * queryValue is what we search for. compareByType dispatches on the element.
     */
    private Truthness computeEqualsAnyElements(List<?> elements, Object queryValue) {
        if (elements.isEmpty()) return FALSE_TRUTHNESS;

        double maxOfTrue = 0.0;
        for (Object element : elements) {
            Truthness t = evaluateEquals(element, queryValue);
            if (t.isTrue()) return TRUE_TRUTHNESS;
            if (t.getOfTrue() > maxOfTrue) maxOfTrue = t.getOfTrue();
        }
        return new Truthness(maxOfTrue, 1.0);
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

    private static List<Map<String, Object>> toList(Iterable<Map<String, Object>> iterable) {
        if (iterable instanceof List) {
            return (List<Map<String, Object>>) iterable;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : iterable) {
            list.add(row);
        }
        return list;
    }
}