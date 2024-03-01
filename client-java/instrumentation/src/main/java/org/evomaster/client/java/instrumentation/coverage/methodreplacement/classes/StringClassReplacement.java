package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;


public class StringClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return String.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean equals(String caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        String left = caller;
        String right = anObject == null ? null : anObject.toString();
        ExecutionTracer.handleTaintForStringEquals(left, right, false);
        ExecutionTracer.handleExtraParamTaint(left, right);
        ExecutionTracer.handleExtraHeaderTaint(left, right);

        //not important if NPE
        boolean result = caller.equals(anObject);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;

        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            if (!(anObject instanceof String)) {
                t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
            } else {
                final double base = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.getLeftAlignmentDistance(caller, anObject.toString());
                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
                t = new Truthness(h, 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean equalsIgnoreCase(String caller, String anotherString, String idTemplate) {
        Objects.requireNonNull(caller);

        ExecutionTracer.handleTaintForStringEquals(caller, anotherString, true);

        //not important if NPE
        boolean result = caller.equalsIgnoreCase(anotherString);


        if (idTemplate == null) {
            return result;
        }

        if (anotherString == null) {
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN,
                    new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1));
            return false;
        }

        Truthness t;

        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double base = DistanceHelper.H_NOT_NULL;
            double distance = DistanceHelper.getLeftAlignmentDistance(
                    caller.toLowerCase(),
                    anotherString.toLowerCase());
            double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
            t = new Truthness(h, 1d);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean startsWith(String caller, String prefix, int toffset, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean result = caller.startsWith(prefix, toffset);

        if (idTemplate == null) {
            return result;
        }

        int pl = prefix.length();

        /*
            The penalty when there is a mismatch of lengths/offset
            should be at least pl, as should be always worse than
            when doing "equals" comparisons.
            Furthermore, need to add extra penalty in case string is
            shorter than prefix
         */
        int penalty = pl;
        if (caller.length() < pl) {
            penalty += (pl - caller.length());
        }

        Truthness t;

        if (toffset < 0) {
            long dist = (-toffset + penalty) * Character.MAX_VALUE;
            t = new Truthness(1d / (1d + dist), 1d);
        } else if (toffset > caller.length() - pl) {
            long dist = (toffset + penalty) * Character.MAX_VALUE;
            t = new Truthness(1d / (1d + dist), 1d);
        } else {
            int len = Math.min(prefix.length(), caller.length());
            String sub = caller.substring(toffset, Math.min(toffset + len, caller.length()));
            return equals(sub, prefix, idTemplate);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean startsWith(String caller, String prefix, String idTemplate) {
        Objects.requireNonNull(caller);

        return startsWith(caller, prefix, 0, idTemplate);
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean endsWith(String caller, String suffix, String idTemplate) {
        Objects.requireNonNull(caller);

        return startsWith(caller, suffix, caller.length() - suffix.length(), idTemplate);
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean isEmpty(String caller, String idTemplate) {
        Objects.requireNonNull(caller);

        if (idTemplate == null) {
            return caller.isEmpty();
        }

        int len = caller.length();
        Truthness t = TruthnessUtils.getTruthnessToEmpty(len);


        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return caller.isEmpty();
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean contentEquals(String caller, CharSequence cs, String idTemplate) {
        if (cs == null) {
            return caller.contentEquals(cs);
        } else {
            return equals(caller, cs.toString(), idTemplate);
        }
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean contentEquals(String caller, StringBuffer sb, String idTemplate) {
        return equals(caller, sb.toString(), idTemplate);
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean contains(String caller, CharSequence s, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean result = caller.contains(s);

        if (idTemplate == null) {
            return result;
        }

        String k = s.toString();
        if (caller.length() <= k.length()) {
            return equals(caller, k, idTemplate);
        }

        Truthness t;

        if (result) {
            t = new Truthness(1, DistanceHelper.H_NOT_NULL);
        } else {
            assert caller.length() > k.length();
            long best = Long.MAX_VALUE;

            for (int i = 0; i < (caller.length() - k.length()) + 1; i++) {
                String sub = caller.substring(i, i + k.length());
                long h = DistanceHelper.getLeftAlignmentDistance(sub, k);
                if (h < best) {
                    best = h;
                }
            }
            t = new Truthness(1d / (1d + best), 1);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean matches(String caller, String regex, String idTemplate) {
        Objects.requireNonNull(caller);
        if (regex == null) {
            // signals a NPE
            return caller.matches(regex);
        } else {
            return PatternMatchingHelper.matches(regex, caller, idTemplate);
        }
    }


    /*
        TODO:
        public boolean regionMatches(int toffset, String other, int ooffset, int len)
        public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len)
     */


}
