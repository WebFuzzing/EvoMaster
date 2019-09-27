package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;

public class FloatClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Float.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static float parseFloat(String input, String idTemplate) {

        if (ExecutionTracer.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.FLOAT, null));
        }

        if (idTemplate == null) {
            return Float.parseFloat(input);
        }

        try {
            float res = Float.parseFloat(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (NumberFormatException | NullPointerException e) {
            double h = parseFloatHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

    /**
     * Optimizes for Java regex pattern "['-']?[0-9]*['.']?[0-9]*"
     * @param input
     * @return
     */
    protected static double parseFloatHeuristic(String input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        final double base = H_NOT_NULL;

        if (input.length() == 0) {
            return base;
        }

        long distance = 0;

        if (input.length() == 1) {
            //cannot be '-'
            distance += distanceToDigit(input.charAt(0));
        } else {

            for (int i = 0; i < input.length(); i++) {

                int digitsDist = distanceToDigit(input.charAt(i));
                if (i == 0) {
                    //first symbol could be a '-'
                    distance += Math.min(digitsDist, distanceToChar(input.charAt(i), '-'));

                } else {
                    int firstIndexOfDot = input.substring(1).indexOf('.');
                    if (firstIndexOfDot != -1) {
                        // optimize for a '.'
                        distance += Math.min(digitsDist, distanceToChar(input.charAt(i), '.'));
                    } else if (i == firstIndexOfDot) {
                        distance += 0;
                    } else {
                        distance += digitsDist;
                    }
                }
            }

        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }

}
