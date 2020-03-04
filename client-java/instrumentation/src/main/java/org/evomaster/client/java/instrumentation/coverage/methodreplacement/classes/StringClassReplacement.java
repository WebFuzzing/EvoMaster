package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Objects;


public class StringClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return String.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equals(String caller, Object anObject, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean taintedCaller = ExecutionTracer.isTaintInput(caller);
        boolean taintedOther = anObject != null && ExecutionTracer.isTaintInput(anObject.toString());

        if (taintedCaller || taintedOther) {
            if (taintedCaller) {
                ExecutionTracer.addStringSpecialization(caller,
                        new StringSpecializationInfo(StringSpecialization.CONSTANT, anObject.toString()));
            } else {
                ExecutionTracer.addStringSpecialization(anObject.toString(),
                        new StringSpecializationInfo(StringSpecialization.CONSTANT, caller));
            }
        }

        //not important if NPE
        boolean result = caller.equals(anObject);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;

        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            if (!(anObject instanceof String)) {
                t = new Truthness(0d, 1d);
            } else {
                long distance = DistanceHelper.getLeftAlignmentDistance(caller, anObject.toString());
                t = new Truthness(1d / (1d + distance), 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean equalsIgnoreCase(String caller, String anotherString, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean taintedCaller = ExecutionTracer.isTaintInput(caller);
        boolean taintedOther = ExecutionTracer.isTaintInput(anotherString);

        if (taintedCaller || taintedOther) {
            if (taintedCaller) {
                ExecutionTracer.addStringSpecialization(caller,
                        new StringSpecializationInfo(StringSpecialization.CONSTANT_IGNORE_CASE, anotherString));
            } else {
                ExecutionTracer.addStringSpecialization(anotherString,
                        new StringSpecializationInfo(StringSpecialization.CONSTANT_IGNORE_CASE, caller));
            }
        }

        //not important if NPE
        boolean result = caller.equalsIgnoreCase(anotherString);


        if (idTemplate == null) {
            return result;
        }

        if (anotherString == null) {
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, new Truthness(0, 1));
            return false;
        }

        Truthness t;

        if (result) {
            t = new Truthness(1d, 0d);
        } else {
            long distance = DistanceHelper.getLeftAlignmentDistance(
                    caller.toLowerCase(),
                    anotherString.toLowerCase());
            t = new Truthness(1d / (1d + distance), 1d);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }


    @Replacement(type = ReplacementType.BOOLEAN)
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

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean startsWith(String caller, String prefix, String idTemplate) {
        Objects.requireNonNull(caller);

        return startsWith(caller, prefix, 0, idTemplate);
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean endsWith(String caller, String suffix, String idTemplate) {
        Objects.requireNonNull(caller);

        return startsWith(caller, suffix, caller.length() - suffix.length(), idTemplate);
    }


    @Replacement(type = ReplacementType.BOOLEAN)
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

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean contentEquals(String caller, CharSequence cs, String idTemplate) {
        if (cs == null) {
            return caller.contentEquals(cs);
        } else {
            return equals(caller, cs.toString(), idTemplate);
        }
    }


    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean contentEquals(String caller, StringBuffer sb, String idTemplate) {
        return equals(caller, sb.toString(), idTemplate);
    }


    @Replacement(type = ReplacementType.BOOLEAN)
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
            t = new Truthness(1, 0);
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

    @Replacement(type = ReplacementType.BOOLEAN)
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
