package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.FloatClassReplacement.parseFloatHeuristic;

public class DoubleClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Double.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static double parseDouble(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.DOUBLE, null));
        }

        if (idTemplate == null) {
            return Double.parseDouble(input);
        }

        try {
            double res = Double.parseDouble(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (NumberFormatException | NullPointerException e) {
            double h = parseFloatHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }


}
