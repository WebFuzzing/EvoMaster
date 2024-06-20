package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper;

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

    public Truthness calculateTruthnessForEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateTruthnessForEquals(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateTruthnessForNotEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateTruthnessForNotEquals(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateTruthnessForGreaterThan(Date left, Date right) {
        return doubleComparisonCalculator.calculateTruthnessForGreaterThan(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateTruthnessForGreaterThanOrEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateTruthnessForGreaterThanOrEquals(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateTruthnessForMinorThan(Date left, Date right) {
        return doubleComparisonCalculator.calculateTruthnessForMinorThan(
            convertToDouble(left), convertToDouble(right));
    }

    public Truthness calculateTruthnessForMinorThanOrEquals(Date left, Date right) {
        return doubleComparisonCalculator.calculateTruthnessForMinorThanOrEquals(
            convertToDouble(left), convertToDouble(right));
    }
}