package org.evomaster.client.java.sql.advanced.helpers.truthness;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;

public class DoubleTruthnessHelper {

    public static Truthness calculateTruthnessForEquals(Double left, Double right) {
        return TruthnessUtils.getEqualityTruthness(left, right);
    }

    public static Truthness calculateTruthnessForNotEquals(Double left, Double right) {
        return calculateTruthnessForEquals(left, right).invert();
    }

    public static Truthness calculateTruthnessForGreaterThan(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(right, left);
    }

    public static Truthness calculateTruthnessForGreaterThanOrEquals(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(left, right).invert();
    }

    public static Truthness calculateTruthnessForMinorThan(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(left, right);
    }

    public static Truthness calculateTruthnessForMinorThanOrEquals(Double left, Double right) {
        return TruthnessUtils.getLessThanTruthness(right, left).invert();
    }
}