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
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;


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

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        if (text != null && ExecutionTracer.isTaintInput(text.toString())) {
            // TODO: Change this to the actual format
            ExecutionTracer.addStringSpecialization(text.toString(),
                    new StringSpecializationInfo(StringSpecialization.DATE_YYYY_MM_DD_HH_SS, null));
        }

        if (idTemplate == null) {
            return LocalDateTime.parse(text);
        }

        try {
            LocalDateTime res = LocalDateTime.parse(text);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (DateTimeParseException e) {
            double h = parseHeuristic(text);
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
