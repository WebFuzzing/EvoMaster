package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;

public class ShortClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Short.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true, category = ReplacementCategory.BASE)
    public static short parseShort(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.INTEGER, null));
        }

        if (idTemplate == null) {
            return Short.parseShort(input);
        }

        try {
            short res = Short.parseShort(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1d, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (RuntimeException e) {
            double h = NumberParsingUtils.parseShortHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1d));
            throw e;
        }
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean equals(Short caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof Short)) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            Short anotherShort = (Short) anObject;
            if (caller.equals(anotherShort)) {
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else {
                double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anotherShort);
                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true, category = ReplacementCategory.BASE)
    public static Short valueOf(String input, String idTemplate) {
        return parseShort(input, idTemplate);
    }
}
