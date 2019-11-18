package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Date;
import java.util.Objects;

public class DateClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Date.class;
    }

    /**
     * Compares two dates for equality. The result is true if and only if the argument
     * is not null and is a Date object that represents the same point in time,
     * to the millisecond, as this object.
     * Thus, two Date objects are equal if and only if the getTime method returns the same
     * long value for both.
     *
     * @param caller
     * @param anObject
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(Date caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.equals(anObject);
        }

        final Truthness t;
        if (anObject == null || !(anObject instanceof Date)) {
            /*
             * TODO: Not sure if arguments of wrong type should be given
             *  the same value as null.
             */
            t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
        } else {
            Date anotherDate = (Date) anObject;
            if (caller.equals(anotherDate)) {
                t = new Truthness(1d, 0d);
            } else {
                double distance = DistanceHelper.getDistanceToEquality(caller, anotherDate);
                final double base = DistanceHelper.H_NOT_NULL;
                double h = base + ((1 - base) / (distance + 1));
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    /**
     * Tests if this date is before the specified date.
     *
     * @param caller
     * @param when
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean before(Date caller, Date when, String idTemplate) {
        Objects.requireNonNull(caller);

        // might throw NPE if when is null
        final boolean res = caller.before(when);
        if (idTemplate == null) {
            return res;
        }

        final Truthness t = getBeforeTruthness(caller, when);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }


    private static Truthness getBeforeTruthness(Date caller, Date when) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(when);

        final long a = caller.getTime();
        final long b = when.getTime();

        /**
         * We use the same gradient that HeuristicsForJumps.getForValueComparison()
         * used for IF_ICMPLT, ie, a < b
         */
        return TruthnessUtils.getLessThanTruthness(a, b);
    }


    /**
     * Tests if this date is after the specified date.
     *
     * @param caller
     * @param when
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean after(Date caller, Date when, String idTemplate) {
        Objects.requireNonNull(caller);

        // might throw NPE if when is null
        final boolean res = caller.after(when);
        if (idTemplate == null) {
            return res;
        }

        final Truthness t = getBeforeTruthness(when, caller);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }


}