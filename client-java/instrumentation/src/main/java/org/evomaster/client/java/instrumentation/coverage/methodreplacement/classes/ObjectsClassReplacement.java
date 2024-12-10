package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;

public class ObjectsClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Objects.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true, category = ReplacementCategory.BASE)
    public static boolean equals(Object left, Object right, String idTemplate) {

        if(left instanceof String && right instanceof String){
            ExecutionTracer.handleTaintForStringEquals((String) left, (String) right, false);
        }

        boolean result = Objects.equals(left, right);
        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            if (left == null || right == null) {
                t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
            } else {
                double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistance(left, right);
                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
                t = new Truthness(h, 1d);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

}
