package org.evomaster.client.java.controller.cassandra.calculator;

import org.evomaster.client.java.controller.cassandra.model.CassandraRow;
import org.evomaster.client.java.controller.cassandra.operations.CqlQueryOperation;
import org.evomaster.client.java.controller.cassandra.parser.CqlParserUtils;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculator designed to compute a branch-distance-based heuristic for a CQL (Cassandra Query Language) query,
 * given the rows returned by executing that query against the database.
 * <p>
 * The distance reflects how far the query's WHERE condition is from being satisfied by
 * the available data and is used to guide the search towards inserting data in the database
 * that makes the executed queries return non-empty results during the execution of generated tests.
 */
public class CassandraHeuristicsCalculator {

    public static final double C        = DistanceHelper.H_NOT_NULL;
    public static final double C_BETTER = 0.15;

    public static final Truthness TRUE_TRUTHNESS         = new Truthness(1.0, C);
    public static final Truthness FALSE_TRUTHNESS        = new Truthness(C, 1.0);
    public static final Truthness FALSE_TRUTHNESS_BETTER = new Truthness(C_BETTER, 1.0);

    private final CassandraOperationEvaluator evaluator = new CassandraOperationEvaluator();

    /**
     * Computes the heuristic distance of the given CQL query with respect to the provided rows.
     * A distance of {@code 0.0} means the query is satisfied, while values closer
     * to {@code 1.0} indicate the query is further from being satisfied.
     *
     * @param cqlQuery the CQL query to evaluate
     * @param returnedRows  all rows in the table the query targets, used to evaluate how close
     *                 the query's condition is to matching at least one row
     * @return a distance value in {@code [0.0, 1.0]}, where {@code 0.0} represents the best case
     */
    public double computeDistance(String cqlQuery, Iterable<CassandraRow> returnedRows) {
        return 1.0 - computeHQuery(cqlQuery, returnedRows).getOfTrue();
    }

    private Truthness computeHQuery(String cqlQuery, Iterable<CassandraRow> returnedRows) {
        if (!CqlParserUtils.canParseCqlCommand(cqlQuery)) {
            return FALSE_TRUTHNESS;
        }

        List<CassandraRow> rows = toList(returnedRows);

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

    private Truthness computeHRowSet(List<CassandraRow> rows) {
        return TruthnessUtils.getTruthnessToEmpty(rows.size()).invert();
    }

    private Truthness computeHCondition(CqlQueryOperation condition,
                                        List<CassandraRow> rows) {
        if (rows.isEmpty()) {
            return FALSE_TRUTHNESS;
        }

        double maxOfTrue = 0.0;
        for (CassandraRow row : rows) {
            double ofTrue = evaluator.evaluate(condition, row).getOfTrue();
            if (ofTrue >= 1.0) {
                return TRUE_TRUTHNESS;
            } else if (ofTrue > maxOfTrue) {
                maxOfTrue = ofTrue;
            }
        }

        return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
    }

    private static List<CassandraRow> toList(Iterable<CassandraRow> iterable) {
        if (iterable instanceof List) {
            return (List<CassandraRow>) iterable;
        }

        List<CassandraRow> list = new ArrayList<>();
        for (CassandraRow row : iterable) {
            list.add(row);
        }
        return list;
    }
}