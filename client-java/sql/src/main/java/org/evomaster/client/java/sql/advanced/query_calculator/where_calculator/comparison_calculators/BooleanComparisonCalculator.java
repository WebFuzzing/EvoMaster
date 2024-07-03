package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;

import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToDouble;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators.DoubleComparisonCalculator.createDoubleComparisonCalculator;

public class BooleanComparisonCalculator {

    private DoubleComparisonCalculator doubleComparisonCalculator;

    private BooleanComparisonCalculator(DoubleComparisonCalculator doubleComparisonCalculator){
        this.doubleComparisonCalculator = doubleComparisonCalculator;
    }

    public static BooleanComparisonCalculator createBooleanComparisonCalculator(){
        return new BooleanComparisonCalculator(createDoubleComparisonCalculator());
    }

    public Truthness calculateTruthnessForEquals(Boolean left, Boolean right) {
        return doubleComparisonCalculator.calculateTruthnessForEquals(convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateTruthnessForNotEquals(Boolean left, Boolean right) {
        return doubleComparisonCalculator.calculateTruthnessForNotEquals(convertToDouble(left), convertToDouble(right));
    }
}