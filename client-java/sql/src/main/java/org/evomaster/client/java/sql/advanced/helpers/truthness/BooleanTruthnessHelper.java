package org.evomaster.client.java.sql.advanced.helpers.truthness;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper;

public class BooleanTruthnessHelper {

    public static Truthness calculateTruthnessForEquals(Boolean left, Boolean right) {
        return DoubleTruthnessHelper.calculateTruthnessForEquals(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }

    public static Truthness calculateTruthnessForNotEquals(Boolean left, Boolean right) {
        return DoubleTruthnessHelper.calculateTruthnessForNotEquals(ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }
}