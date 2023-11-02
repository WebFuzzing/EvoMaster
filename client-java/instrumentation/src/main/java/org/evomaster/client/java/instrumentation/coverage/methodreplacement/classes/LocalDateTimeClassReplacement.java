package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;


public class LocalDateTimeClassReplacement implements MethodReplacementClass {

    private static long getMillis(ChronoLocalDateTime<?> chronoLocalDateTime) {
        return chronoLocalDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static Truthness getIsBeforeTruthness(ChronoLocalDateTime<?> caller, ChronoLocalDateTime<?> when) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(when);

        long a = getMillis(caller);
        long b = getMillis(when);

        /**
         * We use the same gradient that HeuristicsForJumps.getForValueComparison()
         * used for IF_ICMPLT, ie, a < b
         */
        return TruthnessUtils.getLessThanTruthness(a, b);
    }

    @Override
    public Class<?> getTargetClass() {
        return LocalDateTime.class;
    }

    /**
     * Obtains an instance of LocalDateTime from a text string such as 2007-12-03T10:15:30.
     * The string must represent a valid date-time and is parsed using DateTimeFormatter.ISO_LOCAL_DATE_TIME.
     *
     * @param text
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true, category = ReplacementCategory.BASE)
    public static LocalDateTime parse(CharSequence text, String idTemplate) {

        if (text != null && ExecutionTracer.isTaintInput(text.toString())) {
            ExecutionTracer.addStringSpecialization(text.toString(),
                    new StringSpecializationInfo(StringSpecialization.ISO_LOCAL_DATE_TIME, null));
        }

        if (idTemplate == null) {
            return LocalDateTime.parse(text);
        }

        try {
            LocalDateTime res = LocalDateTime.parse(text);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (DateTimeParseException | NullPointerException ex) {
            double h = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing(text);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw ex;
        }
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean equals(LocalDateTime caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof LocalDateTime)) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            LocalDateTime anotherLocalDateTime = (LocalDateTime) anObject;
            if (caller.equals(anotherLocalDateTime)) {
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else {
                double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, anotherLocalDateTime);
                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
                t = new Truthness(h, 1d);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }



    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean isBefore(LocalDateTime caller, ChronoLocalDateTime<?> other, String idTemplate) {
        Objects.requireNonNull(caller);

        // might throw NPE if when is null
        if (idTemplate == null) {
            return caller.isBefore(other);
        }

        Truthness t;
        if (other == null) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            t = getIsBeforeTruthness(caller, other);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.isBefore(other);
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean isAfter(LocalDateTime caller, ChronoLocalDateTime<?> other, String idTemplate) {
        Objects.requireNonNull(caller);

        // might throw NPE if when is null
        if (idTemplate == null) {
            return caller.isAfter(other);
        }
        Truthness t;
        if (other == null) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            t = getIsBeforeTruthness(other, caller);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.isAfter(other);
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean isEqual(LocalDateTime caller, ChronoLocalDateTime<?> other, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.isEqual(other);
        }
        final Truthness t;
        if (other == null) {
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            if (caller.isEqual(other)) {
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else {
                double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getDistanceToEquality(caller, other);
                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
                t = new Truthness(h, 1d);
            }
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.isEqual(other);
    }


}
