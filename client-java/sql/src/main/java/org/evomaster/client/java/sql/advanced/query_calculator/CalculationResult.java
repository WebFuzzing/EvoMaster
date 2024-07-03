package org.evomaster.client.java.sql.advanced.query_calculator;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.driver.row.Row;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.evomaster.client.java.distance.heuristics.Truthness.TRUE;

public class CalculationResult {

    private Truthness truthness;
    private List<Row> rows;

    private CalculationResult(Truthness truthness, List<Row> rows) {
        this.truthness = truthness;
        this.rows = rows;
    }

    public static CalculationResult createCalculationResult(Truthness truthness, List<Row> rows) {
        return new CalculationResult(truthness, rows);
    }

    public static CalculationResult createCalculationResult(Truthness truthness) {
        return createCalculationResult(truthness, emptyList());
    }

    public static CalculationResult createCalculationResult(List<Row> rows) {
        return createCalculationResult(TRUE, rows);
    }

    public Truthness getTruthness() {
        return truthness;
    }

    public List<Row> getRows() {
        return rows;
    }
}
