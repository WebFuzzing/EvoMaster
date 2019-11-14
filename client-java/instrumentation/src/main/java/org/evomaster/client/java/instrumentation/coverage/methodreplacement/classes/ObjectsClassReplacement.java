package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Objects;

public class ObjectsClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Objects.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true)
    public static boolean equals(Object left, Object right, String idTemplate) {

        boolean result = Objects.equals(left, right);
        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            if (left == null || right == null) {
                t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
            } else {
                double base = DistanceHelper.H_NOT_NULL;
                double distance = computeDistance(left, right, idTemplate);
                double h = base + (1d - base) / (1d + distance);
                t = new Truthness(h, 1d);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    private static double computeDistance(Object left, Object right, String idTemplate) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        final double distance;
        if (left instanceof String && right instanceof String) {
            // TODO Add string specialization info for left and right
            String a = (String) left;
            String b = right.toString();
            distance = (double) DistanceHelper.getLeftAlignmentDistance(a, b);

        } else if (left instanceof Integer && right instanceof Integer) {
            int a = (Integer) left;
            int b = (Integer) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof Long && right instanceof Long) {
            long a = (Long) left;
            long b = (Long) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof Float && right instanceof Float) {
            float a = (Float) left;
            float b = (Float) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof Double && right instanceof Double) {
            double a = (Double) left;
            double b = (Double) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof Character && right instanceof Character) {
            Character a = (Character) left;
            Character b = (Character) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof Date && right instanceof Date) {
            Date a = (Date) left;
            Date b = (Date) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof LocalDate && right instanceof LocalDate) {
            LocalDate a = (LocalDate) left;
            LocalDate b = (LocalDate) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof LocalTime && right instanceof LocalTime) {
            LocalTime a = (LocalTime) left;
            LocalTime b = (LocalTime) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else if (left instanceof LocalDateTime && right instanceof LocalDateTime) {
            LocalDateTime a = (LocalDateTime) left;
            LocalDateTime b = (LocalDateTime) right;
            distance = DistanceHelper.getDistanceToEquality(a, b);

        } else {
            distance = Double.MAX_VALUE;
        }

        return distance;
    }
}
