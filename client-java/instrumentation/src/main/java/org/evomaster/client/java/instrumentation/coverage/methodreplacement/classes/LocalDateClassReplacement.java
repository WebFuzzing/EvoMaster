package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.time.LocalDate;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;


public class LocalDateClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return LocalDate.class;
    }

    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static LocalDate parse(CharSequence input, String idTemplate) {

        if(input != null && ExecutionTracer.isTaintInput(input.toString())){
            ExecutionTracer.addStringSpecialization(input.toString(),
                    new StringSpecializationInfo(StringSpecialization.DATE_YYYY_MM_DD, null));
        }

        if (idTemplate == null) {
            return LocalDate.parse(input);
        }

        try {
            LocalDate res = LocalDate.parse(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (RuntimeException e) {
            double h = parseHeuristic(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

    /**
     * returns a value that represents how close is the value to the format YYYY-MM-DD
     *
     * @param input
     * @return
     */
    public static double parseHeuristic(CharSequence input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        try {
            LocalDate.parse(input);
            /*
                due to the simplification later on, still must make
                sure to get a 1 if no exception is thrown
             */
            return 1d;
        } catch (RuntimeException e) {
            //nothing to do
        }

        final double base = H_NOT_NULL;

        long distance = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            // TODO: The code below can be refactored with class DateFormatClassReplacement
            //format YYYY-MM-DD
            if (i >= 0 && i <= 3) {
                //any Y value is ok
                distance += distanceToDigit(c);
            } else if (i == 4 || i == 7) {
                distance += distanceToChar(c, '-');
            } else if (i == 5) {
                //let's simplify and only allow 01 to 09 for MM
                distance += distanceToChar(c, '0');
            } else if (i == 6) {
                distance += distanceToRange(c, '1', '9');
            } else if (i == 8) {
                //let's simplify and only allow 01 to 28
                distance += distanceToRange(c, '0', '2');
            } else if (i == 9) {
                distance += distanceToRange(c, '1', '8');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        if (input.length() < 10) {
            //too short
            distance += (MAX_CHAR_DISTANCE * (10 - input.length()));
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }
}
