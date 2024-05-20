package org.evomaster.client.java.sql.advanced.helpers.truthness;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

public class StringTruthnessHelper {

    public static Truthness calculateTruthnessForEquals(String left, String right) {
        return TruthnessUtils.getStringEqualityTruthness(left, right);
    }

    public static Truthness calculateTruthnessForNotEquals(String left, String right) {
        return calculateTruthnessForEquals(left, right).invert();
    }
}