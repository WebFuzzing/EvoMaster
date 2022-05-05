package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
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

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean containsKey(Map c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        if (idTemplate == null || c instanceof IdentityHashMap) {
            /*
                IdentityHashMap does not use .equals() for the comparisons
             */
            return c.containsKey(o);
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

        CollectionsDistanceUtils.evaluateTaint(keyCollection, o);

        boolean result = keyCollection.contains(o);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(keyCollection, o);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
