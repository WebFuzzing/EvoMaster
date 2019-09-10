package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Collection;

public class CollectionClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Collection.class;
    }


    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean contains(Collection c, Object o, String idTemplate) {

        String inputString = null;
        if (o instanceof String) {
            inputString = (String) o;
        }

        if (TaintInputName.isTaintInput(inputString)) {
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

        Number inputNumber = null;
        if(o instanceof Number){
            inputNumber = (Number) o;
        }

        Truthness t;

        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            if(c.isEmpty()){
                t = new Truthness(0d, 1d);
            } else {
                double max = 0d;

                for(Object value : c){
                    long distance = -1;

                    if(inputString != null && value instanceof String){
                        distance = DistanceHelper.getLeftAlignmentDistance(inputString, (String) value);
                    } else if(inputNumber != null && value instanceof Number){
                        /*
                            TODO would need to support all basic types, eg long and double,
                            but likely would need a rewrite of all distance calculations to use
                            something like BigDecimal, to avoid issues with precision loss
                            and numeric overflows
                        */
                        if(inputNumber instanceof Integer && value instanceof Integer){
                            distance = Math.abs((Integer) inputNumber - (Integer) value);
                        }
                    }

                    if(distance > 0){
                        double h = 1d / (1d + distance);
                        if(h > max){
                            max = h;
                        }
                    }
                }

                assert max < 1d;
                t = new Truthness(max, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }

    /**
     * How close is the collection to being empty?
     *
     * @param caller
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isEmpty(Collection caller, String idTemplate) {

        int len = caller.size();
        Truthness t;
        if (len == 0) {
            t = new Truthness(1, 0);
        } else {
            t = new Truthness(1d / (1d + len), 1);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.isEmpty();
    }
}
