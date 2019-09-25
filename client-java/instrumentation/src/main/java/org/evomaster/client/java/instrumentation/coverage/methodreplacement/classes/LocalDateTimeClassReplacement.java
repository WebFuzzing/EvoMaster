package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DateTimeParsingUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.TruthnessUtils;
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
    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true)
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
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (DateTimeParseException | NullPointerException ex) {
            double h = DateTimeParsingUtils.getDistanceToISOLocalDateTime(text);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw ex;
        }
    }


    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(LocalDateTime caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof LocalDateTime)) {
            t = new Truthness(0d, 1d);
        } else {
            final long a = getMillis(caller);
            final long b = getMillis((LocalDateTime) anObject);
            t = TruthnessUtils.getEqualityTruthness(a, b);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    private static Truthness getIsBeforeTruthness(ChronoLocalDateTime<?> caller, ChronoLocalDateTime<?> when) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(when);

        final long a = getMillis(caller);
        final long b = getMillis(when);

        /**
         * We use the same gradient that HeuristicsForJumps.getForValueComparison()
         * used for IF_ICMPLT, ie, a < b
         */
        return TruthnessUtils.getLessThanTruthness(a, b);
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isBefore(LocalDateTime caller, ChronoLocalDateTime<?> other, String idTemplate) {
        Objects.requireNonNull(caller);

        // might throw NPE if when is null
        final boolean res = caller.isBefore(other);
        if (idTemplate == null) {
            return res;
        }

        final Truthness t = getIsBeforeTruthness(caller, other);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isAfter(LocalDateTime caller, ChronoLocalDateTime<?> other, String idTemplate) {
        Objects.requireNonNull(caller);

        // might throw NPE if when is null
        final boolean res = caller.isAfter(other);
        if (idTemplate == null) {
            return res;
        }

        final Truthness t = getIsBeforeTruthness(other, caller);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean isEqual(LocalDateTime caller, ChronoLocalDateTime<?> other, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.isEqual(other);
        }
        final Truthness t;
        if (other == null) {
            t = new Truthness(0d, 1d);
        } else {
            final long a = getMillis(caller);
            final long b = getMillis(other);
            t = TruthnessUtils.getEqualityTruthness(a, b);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.isEqual(other);
    }

    private static long getMillis(ChronoLocalDateTime<?> chronoLocalDateTime) {
        return chronoLocalDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

}
