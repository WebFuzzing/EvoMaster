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

    public Truthness calculateEquals(Boolean left, Boolean right) {
        return doubleComparisonCalculator.calculateEquals(convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateNotEquals(Boolean left, Boolean right) {
        return doubleComparisonCalculator.calculateNotEquals(convertToDouble(left), convertToDouble(right));
    }
}