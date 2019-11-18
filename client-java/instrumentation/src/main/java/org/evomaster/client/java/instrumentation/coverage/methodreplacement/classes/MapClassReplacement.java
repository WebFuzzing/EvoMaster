package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.CollectionsDistanceUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.*;

public class MapClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Map.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean containsKey(Map c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        if (idTemplate == null || c instanceof IdentityHashMap) {
            /*
                IdentityHashMap does not use .equals() for the comparisons
             */
            return c.containsKey(o);
        }


        String inputString = null;
        if (o instanceof String) {
            inputString = (String) o;
        }


        // keyset() returns an set instance that indirectly calls
        // to containsKey(). In order to avoid a stack overflow
        // we compute a fresh collection
        Collection keyCollection = new HashSet(c.keySet());

        if (ExecutionTracer.isTaintInput(inputString)) {
            for (Object value : keyCollection) {
                if (value instanceof String) {
                    ExecutionTracer.addStringSpecialization(inputString,
                            new StringSpecializationInfo(StringSpecialization.CONSTANT, (String) value));
                }
            }
        }


        boolean result = keyCollection.contains(o);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(keyCollection, o);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
