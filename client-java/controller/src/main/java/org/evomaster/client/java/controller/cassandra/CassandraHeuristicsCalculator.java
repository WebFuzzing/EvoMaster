package org.evomaster.client.java.controller.cassandra;

import org.evomaster.client.java.controller.cassandra.operations.*;
import org.evomaster.client.java.controller.cassandra.parser.CqlParserUtils;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

import java.util.*;

public class CassandraHeuristicsCalculator {

    public static final double C        = DistanceHelper.H_NOT_NULL;
    public static final double C_BETTER = 0.15;

    public static final Truthness TRUE_TRUTHNESS         = new Truthness(1.0, C);
    public static final Truthness FALSE_TRUTHNESS        = new Truthness(C, 1.0);
    public static final Truthness FALSE_TRUTHNESS_BETTER = new Truthness(C_BETTER, 1.0);

    private final CassandraOperationEvaluator evaluator = new CassandraOperationEvaluator();

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
            double ofTrue = evaluator.evaluate(condition, row).getOfTrue();
            if (ofTrue >= 1.0) {
                return TRUE_TRUTHNESS;
            }
            if (ofTrue > maxOfTrue) {
                maxOfTrue = ofTrue;
            }
        }

        return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
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