package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

public class DoubleComparisonCalculator {

    private DoubleComparisonCalculator(){}

    public static DoubleComparisonCalculator createDoubleComparisonCalculator(){
        return new DoubleComparisonCalculator();
    }

    public Truthness calculateTruthnessForEquals(Double left, Double right) {
        return TruthnessUtils.getEqualityTruthness(left, right);
    }

    public Truthness calculateTruthnessForNotEquals(Double left, Double right) {
        return calculateTruthnessForEquals(left, right).invert();
    }

    public Truthness calculateTruthnessForGreaterThan(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(right, left);
    }

    public Truthness calculateTruthnessForGreaterThanOrEquals(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(left, right).invert();
    }

    public Truthness calculateTruthnessForMinorThan(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(left, right);
    }

    public Truthness calculateTruthnessForMinorThanOrEquals(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(right, left).invert();
    }
}