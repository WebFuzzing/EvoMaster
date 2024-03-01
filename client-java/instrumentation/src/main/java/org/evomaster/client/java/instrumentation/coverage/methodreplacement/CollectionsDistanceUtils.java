package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Collection;
import java.util.Objects;

public abstract class CollectionsDistanceUtils {

    private static final int SCAN_LIMIT = 50;


    public static void evaluateTaint(Collection c, Object o){
        String inputString = null;
        if (o instanceof String) {
            inputString = (String) o;
        } else {
            return;
        }

        if(inputString.equals(TaintInputName.EXTRA_PARAM_TAINT)){
            for(Object value : c){
                if(value instanceof String){
                    ExecutionTracer.addQueryParameter((String) value);
                } else {
                    return; //collections is not of strings
                }
            }
        }

        if(inputString.equals(TaintInputName.EXTRA_HEADER_TAINT)){
            for(Object value : c){
                if(value instanceof String){
                    ExecutionTracer.addHeader((String) value);
                } else {
                    return; //collections is not of strings
                }
            }
        }

        if(c.size() <= 16){
            /*
                cannot search indiscriminately, as we do not know performance of search, eg could be O(n),
                making this a major a bottleneck.
                As anyway should not have too many query params, the size of collection should be short
             */
            /*
                Cannot call c.contains, as could be a delegate which is instrumented, which would end up
                always adding the content as an extra param
             */
//            if(c.contains(TaintInputName.EXTRA_PARAM_TAINT)){
//                ExecutionTracer.addQueryParameter(inputString);
//            }
            for(Object value : c){
                if(! (value instanceof String)){
                    return;
                } else {
                    if(value.equals(TaintInputName.EXTRA_PARAM_TAINT)) {
                        ExecutionTracer.addQueryParameter(inputString);
                        break;
                    }
                    if(value.equals(TaintInputName.EXTRA_HEADER_TAINT)) {
                        ExecutionTracer.addHeader(inputString);
                        break;
                    }
                }
            }
        }


        if (ExecutionTracer.isTaintInput(inputString)) {
            int counter = 0;

            for (Object value : c) {
                if (value instanceof String) {
                    ExecutionTracer.addStringSpecialization(inputString,
                            new StringSpecializationInfo(StringSpecialization.CONSTANT, (String) value));
                    counter++;
                    if(counter >= 10){
                        return;
                    }
                }  else {
                    return;
                }
            }
        }
    }

    public static double getHeuristicToContains(Collection c, Object o) {
        return getHeuristicToContains(c, o, SCAN_LIMIT);
    }

    public static double getHeuristicToContainsAll(Collection c, Collection other) {
        return getHeuristicToContainsAll(c, other, SCAN_LIMIT);
    }

    public static double getHeuristicToContainsAny(Collection c, Collection other){
        return getHeuristicToContainsAny(c, other, SCAN_LIMIT);
    }

    /**
     *  There must be at least 1 element in 'other' that is inside 'c'
     */
    public static double getHeuristicToContainsAny(Collection c, Collection other, int limit){
        Objects.requireNonNull(c);

        double base = DistanceHelper.H_REACHED_BUT_EMPTY / 3d;

        if(c.isEmpty()){
            return base;
        }
        if(other == null){
            return base * 2;
        }
        if(other.isEmpty()){
            return  base * 3;
        }

        double max = DistanceHelper.H_REACHED_BUT_EMPTY;
        for(Object obj : other){
            max = Math.max(max, getHeuristicToContains(c,obj, limit));
            if(max == 1d){
                return 1d;
            }
        }

        return max;
    }

    public static double getHeuristicToContainsAll(Collection c, Collection other, int limit) {
        Objects.requireNonNull(c);

        boolean result = c.containsAll(other);

        if (result) {
            return 1d;
        } else if(c.isEmpty()){
            return DistanceHelper.H_REACHED_BUT_EMPTY;
        }

        assert c!=null && other!=null && !other.isEmpty(); // otherwise function would had returned or exception

        double sum = 0d;
        //TODO should the "limit" applied to "other" as well?
        for(Object x : other.toArray()){
            sum += getHeuristicToContains(c, x, limit);
        }
        sum = sum / (double) (other.size() + Math.log(other.size())); // punish more elements

        assert sum >=0 && sum <= 1;

        double h = DistanceHelper.scaleHeuristicWithBase(sum, DistanceHelper.H_REACHED_BUT_EMPTY);

        return h;
     }

    /**
     * Compute distance of object from each one of the elements in the collection.
     * But look only up to limit elements.
     * A negative values means look at all elements
     */
    public static double getHeuristicToContains(Collection c, Object o, int limit) {
        Objects.requireNonNull(c);

        boolean result = c.contains(o);
        if (result) {
            return 1d;
        } else if (c.isEmpty()) {
            return DistanceHelper.H_REACHED_BUT_EMPTY;
        } else if (o == null){
            //null gives no gradient
            return DistanceHelper.H_NOT_EMPTY;
        }else {

            int counter = 0;

            final double base = DistanceHelper.H_NOT_EMPTY;
            double max = base;

            for (Object value : c) {
                if (counter == limit) {
                    break;
                }
                counter++;
                if(value == null){
                    continue;
                }

                double distance = DistanceHelper.getDistance(o, value);
                if(distance == Double.MAX_VALUE){
                    continue;
                }

                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);

                if (h > max) {
                    max = h;
                }
            }
            assert max < 1d;
            return max;
        }

    }
}
