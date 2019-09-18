package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.IntegerClassReplacement.parseLongHeuristic;

public class LongClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Long.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static long parseLong(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.LONG, null));
        }

        if (idTemplate == null) {
            return Long.parseLong(input);
        }

        try {
            long res = Long.parseLong(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (RuntimeException e) {
            double h = parseLongHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }


}
