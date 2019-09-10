package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
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
            if (left != null && right != null && left instanceof String) {
                // string equals
                String caller = (String) left;
                long distance = DistanceHelper.getLeftAlignmentDistance(caller, right.toString());
                t = new Truthness(1d / (1d + distance), 1d);
            } else {
                // can't apply string equals
                t = new Truthness(0d, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
