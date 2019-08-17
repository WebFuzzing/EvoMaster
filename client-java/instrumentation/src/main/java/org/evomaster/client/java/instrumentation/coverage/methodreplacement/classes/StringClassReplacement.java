package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;


import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement.TYPE;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;


public class StringClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return String.class;
    }

    @Replacement(type = TYPE.BOOLEAN)
    public static boolean equals(String caller, Object anObject, String idTemplate) {

        //not important if NPE
        boolean result = caller.equals(anObject);

        Truthness t;

        if(result){
            t = new Truthness(1d, 0d);
        } else {
            if (!(anObject instanceof String)) {
                t = new Truthness(0d, 1d);
            } else {
                long distance = getLeftAlignmentDistance(caller, anObject.toString());
                t = new Truthness(1d / (1d + distance), 1d);
            }
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, TYPE.BOOLEAN, t);

        return result;
    }

    @Replacement(type = Replacement.TYPE.BOOLEAN)
    public static boolean equalsIgnoreCase(String caller, String anotherString, String idTemplate) {

        if(anotherString == null){
            ExecutionTracer.executedReplacedMethod(idTemplate, TYPE.BOOLEAN, new Truthness(0,1));
            return false;
        }

        return equals(caller.toLowerCase(), anotherString.toLowerCase(), idTemplate);
    }

//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int startsWith(String caller, String prefix, int toffset) {
//        Objects.requireNonNull(caller);
//        Objects.requireNonNull(prefix);
//
//        int pl = prefix.length();
//
//        /*
//            The penalty when there is a mismatch of lengths/offset
//            should be at least pl, as should be always worse than
//            when doing "equals" comparisons.
//            Furthermore, need to add extra penalty in case string is
//            shorter than prefix
//         */
//        int penalty = pl;
//        if(caller.length() < pl){
//            penalty += (pl - caller.length());
//        }
//
//        if (toffset < 0) {
//            long dist = (-toffset + penalty) * Character.MAX_VALUE;
//            return truncate(-dist);
//        }
//
//        if (toffset > caller.length() - pl) {
//            assert toffset >= 0;
//            long dist = (toffset + penalty) * Character.MAX_VALUE;
//            return truncate(-dist);
//        }
//
//        int len = Math.min(prefix.length(), caller.length());
//        String sub = caller.substring(toffset, Math.min(toffset + len, caller.length()));
//
//        return equals(sub, prefix);
//    }
//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int startsWith(String caller, String prefix) {
//        return startsWith(caller, prefix, 0);
//    }
//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int endsWith(String caller, String suffix) {
//        return startsWith(caller, suffix, caller.length() - suffix.length());
//    }
//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int isEmpty(String caller) {
//        Objects.requireNonNull(caller);
//
//        int len = caller.length();
//        if (len == 0) {
//            return BooleanReplacement.TRUE_MAX;
//        } else {
//            return -len;
//        }
//    }
//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int contentEquals(String caller, CharSequence cs) {
//        return equals(caller, cs.toString());
//    }
//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int contentEquals(String caller, StringBuffer sb) {
//        return equals(caller, sb.toString());
//    }
//
//    @Replacement(type = Replacement.TYPE.BOOLEAN)
//    public static int contains(String caller, CharSequence s) {
//        Objects.requireNonNull(caller);
//        Objects.requireNonNull(s);
//
//        if (caller.contains(s)) {
//            return BooleanReplacement.TRUE_MAX;
//        }
//
//        String k = s.toString();
//        if (caller.length() <= k.length()) {
//            return equals(caller, k);
//        }
//
//        assert caller.length() > k.length();
//        int best = BooleanReplacement.FALSE_MIN;
//        for (int i = 0; i < (caller.length() - k.length()) + 1; i++) {
//            String sub = caller.substring(i, i + k.length());
//            int h = equals(sub, k);
//            if (h > best) {
//                best = h;
//            }
//        }
//        return best;
//    }

    /*
        TODO:
        public boolean regionMatches(int toffset, String other, int ooffset, int len)
        public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len)
        public boolean matches(String regex)
     */


    public static long getLeftAlignmentDistance(String a, String b) {

        long diff = Math.abs(a.length() - b.length());
        long dist = diff * Character.MAX_VALUE;

        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            dist += Math.abs(a.charAt(i) - b.charAt(i));
        }

        assert dist >= 0;
        return dist;
    }
}
