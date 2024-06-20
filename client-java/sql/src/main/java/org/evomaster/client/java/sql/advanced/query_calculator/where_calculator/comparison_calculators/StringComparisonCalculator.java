package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.comparison_calculators;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

public class StringComparisonCalculator {

    private TaintHandler taintHandler;

    private StringComparisonCalculator(TaintHandler taintHandler){
        this.taintHandler = taintHandler;
    }

    public static StringComparisonCalculator createStringComparisonCalculator(TaintHandler taintHandler){
        return new StringComparisonCalculator(taintHandler);
    }

    public Truthness calculateTruthnessForEquals(String left, String right) {
        if(taintHandler != null){
            taintHandler.handleTaintForStringEquals(left, right, false);
        }
        return TruthnessUtils.getStringEqualityTruthness(left, right);
    }

    public Truthness calculateTruthnessForNotEquals(String left, String right) {
        return calculateTruthnessForEquals(left, right).invert();
    }
}