package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
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
    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean contains(Collection c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        CollectionsDistanceUtils.evaluateTaint(c,o);

        boolean result = c.contains(o);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(c, o);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0)
    public static boolean containsAll(Collection caller, Collection other, String idTemplate) {
        Objects.requireNonNull(caller);

        if(other != null && !other.isEmpty()){
            for(Object obj : other){
                CollectionsDistanceUtils.evaluateTaint(caller,obj);
            }
        }

        boolean result = caller.containsAll(other);
        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContainsAll(caller, other);
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
    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
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

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0, isPure = false)
    public static boolean remove(Collection caller, Object obj, String idTemplate){
        Objects.requireNonNull(caller);

        CollectionsDistanceUtils.evaluateTaint(caller,obj);

        boolean result = caller.remove(obj);
        if (idTemplate == null) {
            return result;
        }

        //note: here we cannot call directly contains(), as remove() might have changed the container

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            //element was not removed, so not contained
            double h = CollectionsDistanceUtils.getHeuristicToContains(caller, obj);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0, isPure = false)
    public static boolean removeAll(Collection caller, Collection other, String idTemplate){
        Objects.requireNonNull(caller);

        if(other != null && !other.isEmpty()){
            for(Object obj : other){
                CollectionsDistanceUtils.evaluateTaint(caller,obj);
            }
        }

        boolean result = caller.removeAll(other);
        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            //no element was removed, so not contained
            double h = CollectionsDistanceUtils.getHeuristicToContainsAny(caller, other);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }
}
