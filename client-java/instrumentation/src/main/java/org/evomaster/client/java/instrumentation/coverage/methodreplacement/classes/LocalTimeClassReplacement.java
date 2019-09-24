package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;


public class LocalTimeClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return LocalTime.class;
    }

    /**
     * Parses a time without an offset, such as '10:15' or '10:15:30'.
     *
     * @param input
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
    public static LocalTime parse(CharSequence input, String idTemplate) {

        if (input != null && ExecutionTracer.isTaintInput(input.toString())) {
            ExecutionTracer.addStringSpecialization(input.toString(),
                    new StringSpecializationInfo(StringSpecialization.ISO_LOCAL_TIME, null));
        }

        if (idTemplate == null) {
            return LocalTime.parse(input);
        }

        try {
            LocalTime res = LocalTime.parse(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (DateTimeParseException | NullPointerException e) {
            double h = getDistanceToLocalTimeWithOrWithoutSeconds(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

    private static final int EXPECTED_SIZE_OF_INPUT = "HH:MM:SS".length();

    private static double getDistanceToLocalTimeWithOrWithoutSeconds(CharSequence input) {
        return Math.min(
                getDistanceToLocalTimeWithoutSeconds(input),
                getDistanceToLocalTimeWithSeconds(input));
    }

    private static double getDistanceToLocalTimeWithoutSeconds(CharSequence input) {
        return getDistanceToLocalTimeWithSeconds(input == null ? null : input + ":00");
    }

    /**
     * returns a value that represents how close is the input to the format HH:MM or HH:MM:SS
     *
     * @param input
     * @return
     */
    private static double getDistanceToLocalTimeWithSeconds(CharSequence input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        final double base = H_NOT_NULL;

        long distance = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            //format HH:MM:SS
            //let's simplify and only allow 00:00:00 to 19:59:59

            if (i == 0) {
                distance += distanceToRange(c, '0', '1');
            } else if (i == 1 || i == 4 || i == 7) {
                distance += distanceToRange(c, '0', '9');
            } else if (i == 2 || i == 5) {
                distance += distanceToChar(c, ':');
            } else if (i == 3 || i == 6) {
                distance += distanceToRange(c, '0', '5');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        if (input.length() < EXPECTED_SIZE_OF_INPUT) {
            //too short
            distance += (MAX_CHAR_DISTANCE * (EXPECTED_SIZE_OF_INPUT - input.length()));
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(LocalTime caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof LocalTime)) {
            t = new Truthness(0d, 1d);
        } else {
            final long a = caller.toSecondOfDay();
            final long b = ((LocalTime) anObject).toSecondOfDay();
            t = TruthnessUtils.getEqualityTruthness(a, b);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isBefore(LocalTime caller, LocalTime when, String idTemplate) {
        Objects.requireNonNull(caller);
        return LocalDateTimeClassReplacement.isBefore(
                toLocalDateTime(caller),
                when == null ? null : toLocalDateTime(when),
                idTemplate);

    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isAfter(LocalTime caller, LocalTime when, String idTemplate) {
        Objects.requireNonNull(caller);
        return LocalDateTimeClassReplacement.isAfter(
                toLocalDateTime(caller),
                when == null ? null : toLocalDateTime(when),
                idTemplate);
    }

    private static final LocalDate LOCAL_DATE = LocalDate.of(1970, 1, 1);

    private static LocalDateTime toLocalDateTime(LocalTime localTime) {
        Objects.requireNonNull(localTime);
        return localTime.atDate(LOCAL_DATE);
    }

}
