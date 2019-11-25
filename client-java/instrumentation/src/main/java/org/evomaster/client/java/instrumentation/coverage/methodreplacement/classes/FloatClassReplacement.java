package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.NumberParsingUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;

public class FloatClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Float.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static float parseFloat(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.FLOAT, null));
        }

        if (idTemplate == null) {
            return Float.parseFloat(input);
        }

        try {
            float res = Float.parseFloat(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (NumberFormatException | NullPointerException e) {
            double h = NumberParsingUtils.getParsingHeuristicValueForFloat(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(Float caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof Float)) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            Float anotherFloat = (Float) anObject;
            if (caller.equals(anotherFloat)) {
                t = new Truthness(1d, 0d);
            } else {
                final double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anotherFloat);
                double h = base + ((1 - base) / (distance + 1));
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static float valueOf(String input, String idTemplate) {
        return parseFloat(input, idTemplate);
    }
}
