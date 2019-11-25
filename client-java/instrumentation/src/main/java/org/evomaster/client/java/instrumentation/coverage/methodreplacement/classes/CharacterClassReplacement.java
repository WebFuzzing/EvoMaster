package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;

public class CharacterClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Character.class;
    }


    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(Character caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof Character)) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            Character anoterCharacter = (Character) anObject;
            if (caller.equals(anoterCharacter)) {
                t = new Truthness(1d, 0d);
            } else {
                final double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anoterCharacter);
                double h = base + ((1 - base) / (distance + 1));
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }


}
