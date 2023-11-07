package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;


public class LocalTimeClassReplacement implements MethodReplacementClass {

    private static final LocalDate LOCAL_DATE = LocalDate.of(1970, 1, 1);

    private static LocalDateTime toLocalDateTime(LocalTime localTime) {
        Objects.requireNonNull(localTime);
        return localTime.atDate(LOCAL_DATE);
    }

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
    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true, category = ReplacementCategory.BASE)
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
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (DateTimeParseException | NullPointerException e) {
            double h = DateTimeParsingUtils.getHeuristicToISOLocalTimeParsing(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean equals(LocalTime caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof LocalTime)) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            LocalTime anotherLocalTime = (LocalTime) anObject;
            if (caller.equals(anotherLocalTime)) {
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else {
                double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anotherLocalTime);
                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
                t = new Truthness(h, 1d);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean isBefore(LocalTime caller, LocalTime when, String idTemplate) {
        Objects.requireNonNull(caller);
        return LocalDateTimeClassReplacement.isBefore(
                toLocalDateTime(caller),
                when == null ? null : toLocalDateTime(when),
                idTemplate);

    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean isAfter(LocalTime caller, LocalTime when, String idTemplate) {
        Objects.requireNonNull(caller);
        return LocalDateTimeClassReplacement.isAfter(
                toLocalDateTime(caller),
                when == null ? null : toLocalDateTime(when),
                idTemplate);
    }


}
