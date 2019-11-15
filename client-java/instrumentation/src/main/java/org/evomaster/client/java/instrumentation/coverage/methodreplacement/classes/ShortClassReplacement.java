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

public class ShortClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Short.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
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
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1d, 0d));
            return res;
        } catch (RuntimeException e) {
            double h = NumberParsingUtils.parseShortHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1d));
            throw e;
        }
    }

    @Replacement(type = ReplacementType.BOOLEAN)
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
                t = new Truthness(1d, 0d);
            } else {
                final double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anotherShort);
                double h = base + ((1 - base) / (distance + 1));
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static short valueOf(String input, String idTemplate) {
        return parseShort(input, idTemplate);
    }
}
