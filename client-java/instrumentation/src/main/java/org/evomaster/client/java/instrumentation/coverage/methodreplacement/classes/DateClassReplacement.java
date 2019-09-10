package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
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
        final Truthness t = getEqualsTruthness(caller, anObject);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.equals(anObject);
    }

    /**
     * Returns the truthness value for an expression date.equals(date)
     *
     * @param caller
     * @param anObject
     * @return
     */
    static Truthness getEqualsTruthness(Date caller, Object anObject) {
        final Truthness t;
        if (anObject == null || !(anObject instanceof Date)) {
            t = new Truthness(0d, 1d);
        } else {
            final long a = caller.getTime();
            final long b = ((Date) anObject).getTime();
            t = new Truthness(
                    1d - Truthness.normalizeValue(Math.abs(a - b)),
                    a != b ? 1d : 0d);
        }
        return t;
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
        // might throw NPE if when is null
        final boolean res = caller.before(when);

        final Truthness t = getBeforeTruthness(caller, when);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }


    static Truthness getBeforeTruthness(Date caller, Date when) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(when);

        final long a = caller.getTime();
        final long b = when.getTime();

        /**
         * We use the same gradient that HeuristicsForJumps.getForValueComparison()
         * used for IF_ICMPLT, ie, a < b
         */
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + a - b),
                a >= b ? 1d : 1d / (1.1d + b - a)
        );
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
        // might throw NPE if when is null
        final boolean res = caller.after(when);

        final Truthness t = getBeforeTruthness(when, caller);
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return res;
    }

}