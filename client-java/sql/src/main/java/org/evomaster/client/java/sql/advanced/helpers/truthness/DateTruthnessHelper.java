package org.evomaster.client.java.sql.advanced.helpers.truthness;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper;

import java.util.Date;

public class DateTruthnessHelper {

    public static Truthness calculateTruthnessForEquals(Date left, Date right) {
        return DoubleTruthnessHelper.calculateTruthnessForEquals(
            ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }

    public static Truthness calculateTruthnessForNotEquals(Date left, Date right) {
        return DoubleTruthnessHelper.calculateTruthnessForNotEquals(
            ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }

    public static Truthness calculateTruthnessForGreaterThan(Date left, Date right) {
        return DoubleTruthnessHelper.calculateTruthnessForGreaterThan(
            ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }

    public static Truthness calculateTruthnessForGreaterThanOrEquals(Date left, Date right) {
        return DoubleTruthnessHelper.calculateTruthnessForGreaterThanOrEquals(
            ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }

    public static Truthness calculateTruthnessForMinorThan(Date left, Date right) {
        return DoubleTruthnessHelper.calculateTruthnessForMinorThan(
            ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }

    public static Truthness calculateTruthnessForMinorThanOrEquals(Date left, Date right) {
        return DoubleTruthnessHelper.calculateTruthnessForMinorThanOrEquals(
            ConversionsHelper.convertToDouble(left), ConversionsHelper.convertToDouble(right));
    }
}