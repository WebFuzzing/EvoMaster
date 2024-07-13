package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

public class DoubleComparisonCalculator {

    private DoubleComparisonCalculator(){}

    public static DoubleComparisonCalculator createDoubleComparisonCalculator(){
        return new DoubleComparisonCalculator();
    }

    public Truthness calculateEquals(Double left, Double right) {
        return TruthnessUtils.getEqualityTruthness(left, right);
    }

    public Truthness calculateNotEquals(Double left, Double right) {
        return calculateEquals(left, right).invert();
    }

    public Truthness calculateGreaterThan(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(right, left);
    }

    public Truthness calculateGreaterThanOrEquals(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(left, right).invert();
    }

    public Truthness calculateMinorThan(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(left, right);
    }

    public Truthness calculateMinorThanOrEquals(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(right, left).invert();
    }
}