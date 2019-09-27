package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;

public class IntegerClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Integer.class;
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static int parseInt(String input, String idTemplate) {

        if(ExecutionTracer.isTaintInput(input)){
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.INTEGER, null));
        }

        if (idTemplate == null) {
            return Integer.parseInt(input);
        }

        try {
            int res = Integer.parseInt(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (RuntimeException e) {
            double h = parseIntHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

    private static double parseIntHeuristic(String input, int maxNumberOfDigits) {

        if (maxNumberOfDigits < 0) {
            throw new IllegalArgumentException("Number of digits cannot be negative");
        }

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
                } else if (i >= maxNumberOfDigits) {

                    //too long string would not be a valid 32bit/64bit integer representation
                    distance += MAX_CHAR_DISTANCE;
                } else {
                    distance += digitsDist;
                }

            }
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }


    public static double parseIntHeuristic(String input) {
        final int maxNumberOfDigits = Integer.valueOf(Integer.MIN_VALUE).toString().length();
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseLongHeuristic(String input) {
        final int maxNumberOfDigits = Long.valueOf(Long.MIN_VALUE).toString().length();
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

}
