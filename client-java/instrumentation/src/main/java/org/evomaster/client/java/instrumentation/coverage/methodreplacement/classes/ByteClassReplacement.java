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

public class ByteClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Byte.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static byte parseByte(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.INTEGER, null));
        }

        if (idTemplate == null) {
            return Byte.parseByte(input);
        }

        try {
            byte res = Byte.parseByte(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1d, 0d));
            return res;
        } catch (RuntimeException e) {
            double h = NumberParsingUtils.parseByteHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1d));
            throw e;
        }
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(Byte caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof Byte)) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            Byte anoterByte = (Byte) anObject;
            if (caller.equals(anoterByte)) {
                t = new Truthness(1d, 0d);
            } else {
                final double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anoterByte);
                double h = base + ((1 - base) / (distance + 1));
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static byte valueOf(String input, String idTemplate) {
        return parseByte(input, idTemplate);
    }

}