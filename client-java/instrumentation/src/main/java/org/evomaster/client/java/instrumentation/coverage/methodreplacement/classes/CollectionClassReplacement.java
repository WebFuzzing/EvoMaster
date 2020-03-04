package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Collection;
import java.util.Objects;

public class CollectionClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Collection.class;
    }


    /**
     * @param c
     * @param o
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean contains(Collection c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        String inputString = null;
        if (o instanceof String) {
            inputString = (String) o;
        }

        if (ExecutionTracer.isTaintInput(inputString)) {
            for (Object value : c) {
                if (value instanceof String) {
                    ExecutionTracer.addStringSpecialization(inputString,
                            new StringSpecializationInfo(StringSpecialization.CONSTANT, (String) value));
                }
            }
        }

        boolean result = c.contains(o);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(c, o);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    /**
     * This function is called only when the caller is non-null.
     * The heuristic value is 1/(1+c.size()) where c!=null.
     * <p>
     * The closer the heuristic value is to 1, the closer the collection
     * is of being empty.
     *
     * @param caller     a non-null Collection instance
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isEmpty(Collection caller, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean result = caller.isEmpty();
        if (idTemplate == null) {
            return result;
        }

        int len = caller.size();
        Truthness t = TruthnessUtils.getTruthnessToEmpty(len);

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
