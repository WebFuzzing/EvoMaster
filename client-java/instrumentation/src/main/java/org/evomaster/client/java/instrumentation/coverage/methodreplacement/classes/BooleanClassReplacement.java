package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.H_REACHED_BUT_NULL;

public class BooleanClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Boolean.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, replacingStatic = true)
    public static boolean parseBoolean(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.BOOLEAN, null));
        }

        if (idTemplate == null) {
            return Boolean.parseBoolean(input);
        }

        boolean res = Boolean.parseBoolean(input);
        Truthness t;
        if (res) {
            t = new Truthness(1, 0);
        } else {
            if (input == null) {
                t = new Truthness(0, 1);
            } else {
                long distance = DistanceHelper.getLeftAlignmentDistance(input.toLowerCase(), "true");
                double h = 1d / (1d + distance);
                t = new Truthness(h, 1);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }
}
