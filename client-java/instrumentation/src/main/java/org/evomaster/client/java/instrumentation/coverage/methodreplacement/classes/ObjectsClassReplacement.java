package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

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
            if (left != null && right != null) {
                t = computeDistance(left, right);
            } else {
                // can't apply known heuristics with null values
                t = new Truthness(0d, 1d);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    private static Truthness computeDistance(Object left, Object right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        if (left instanceof String && right instanceof String) {

            // string equals
            String caller = (String) left;
            long distance = DistanceHelper.getLeftAlignmentDistance(caller, right.toString());
            return new Truthness(1d / (1d + distance), 1d);

        } else if (left instanceof Integer && right instanceof Integer) {
            int caller = (Integer) left;
            int input = (Integer) right;
            return TruthnessUtils.getEqualityTruthness(caller, input);

        } else if (left instanceof Long && right instanceof Long) {

            long caller = (Long) left;
            long input = (Long) right;
            return TruthnessUtils.getEqualityTruthness(caller, input);

        } else if (left instanceof Float && right instanceof Float) {

            float caller = (Float) left;
            float input = (Float) right;
            return TruthnessUtils.getEqualityTruthness(caller, input);


        } else if (left instanceof Double && right instanceof Double) {
            double caller = (Double) left;
            double input = (Double) right;
            return TruthnessUtils.getEqualityTruthness(caller, input);

        } else if (left instanceof Character && right instanceof Character) {

            long distance = DistanceHelper.getLeftAlignmentDistance(left.toString(), right.toString());
            return new Truthness(1d / (1d + distance), 1d);

        } else {
            // can't get any guidance when for other data types
            return new Truthness(0d, 1d);
        }
    }
}
