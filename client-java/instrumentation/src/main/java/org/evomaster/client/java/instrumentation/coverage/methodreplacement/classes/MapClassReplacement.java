package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.CollectionsDistanceUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

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


        /*
            keySet() returns a set instance that indirectly calls
            to containsKey() when doing a contains().
            In order to avoid a stack overflow in classes sub-classing JDK
            collections, we compute a fresh collection.
            An example is StringHashMap from RestAssured.

            The problem though is this can be come quickly very, very
            expensive...

            NOTE: due to changes in ReplacementList, this does not seem a problem
            anymore. See comments in that class.
         */
        //Collection keyCollection = new HashSet(c.keySet());
        Collection keyCollection = c.keySet();

        if (ExecutionTracer.isTaintInput(inputString)) {
            int counter = 0;
            for (Object value : keyCollection) {
                if (value instanceof String) {
                    ExecutionTracer.addStringSpecialization(inputString,
                            new StringSpecializationInfo(StringSpecialization.CONSTANT, (String) value));
                    counter++;
                    if(counter >= 10){
                        //no point in creating possibly hundreds/thousands of constants...
                        break;
                    }
                }
            }
        }


        boolean result = keyCollection.contains(o);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(keyCollection, o, 50);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
