package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;

import java.util.Date;

import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToDouble;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators.DoubleComparisonCalculator.createDoubleComparisonCalculator;

public class DateComparisonCalculator {

    private DoubleComparisonCalculator doubleComparisonCalculator;

    private DateComparisonCalculator(DoubleComparisonCalculator doubleComparisonCalculator){
        this.doubleComparisonCalculator = doubleComparisonCalculator;
    }

    public static DateComparisonCalculator createDateComparisonCalculator(){
        return new DateComparisonCalculator(createDoubleComparisonCalculator());
    }

    public Truthness calculateEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateEquals(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateNotEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateNotEquals(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateGreaterThan(Date left, Date right) {
        return doubleComparisonCalculator.calculateGreaterThan(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateGreaterThanOrEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateGreaterThanOrEquals(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateMinorThan(Date left, Date right) {
        return doubleComparisonCalculator.calculateMinorThan(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateMinorThanOrEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateMinorThanOrEquals(
            convertToDouble(left), convertToDouble(right));
    }
}