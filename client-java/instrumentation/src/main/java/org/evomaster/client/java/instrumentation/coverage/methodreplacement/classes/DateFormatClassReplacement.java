package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.MAX_CHAR_DISTANCE;

public class DateFormatClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return DateFormat.class;
    }

    @Replacement(type = ReplacementType.EXCEPTION)
    public static Date parse(DateFormat caller, String input, String idTemplate) throws ParseException {
        Objects.requireNonNull(caller);

        if (TaintInputName.isTaintInput(input)) {
            ExecutionTracer.addStringSpecialization(input,
                    new StringSpecializationInfo(StringSpecialization.DATE_YYYY_MM_DD, null));
        }

        if (idTemplate == null) {
            return caller.parse(input);
        }

        try {
            Date res = caller.parse(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (ParseException e) {
            Truthness t;
            if (caller instanceof SimpleDateFormat && ((SimpleDateFormat) caller).toPattern().equals("YYYY-MM-DD HH:SS")) {
                double h = parseHeuristicDateTime(input);
                t = new Truthness(h, 1);
            } else {
                t = new Truthness(0, 1);
            }
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, t);
            throw e;
        }
    }

    /**
     * returns a value that represents how close is the value to the format YYYY-MM-DD
     *
     * @param input
     * @return
     */
    public static double parseHeuristicDateTime(CharSequence input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        try {
            new SimpleDateFormat("YYYY-MM-DD HH:MM").parse(input.toString());
            /*
                due to the simplification later on, still must make
                sure to get a 1 if no exception is thrown
             */
            return 1d;
        } catch (ParseException e) {
            //nothing to do
        }

        final double base = H_NOT_NULL;

        long distance = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            //format YYYY-MM-DD HH:MM

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
            } else if (i == 10) {
                distance += distanceToChar(c, ' ');
            } else if (i == 11) {
                // let's simplify and only allow 00 to 19 for HH
                distance += distanceToRange(c, '0', '1');
            } else if (i == 12) {
                distance += distanceToRange(c, '0', '9');
            } else if (i == 13) {
                distance += distanceToChar(c, ':');
            } else if (i == 14) {
                // allow 00 to 59 for MM
                distance += distanceToRange(c, '0', '5');
            } else if (i == 15) {
                distance += distanceToRange(c, '0', '9');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        int requiredLength = "YYYY-MM-DD HH:MM".length();
        if (input.length() < requiredLength) {
            //too short
            distance += (MAX_CHAR_DISTANCE * (requiredLength - input.length()));
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }
}
