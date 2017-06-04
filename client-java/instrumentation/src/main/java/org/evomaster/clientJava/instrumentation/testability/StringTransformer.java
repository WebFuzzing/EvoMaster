package org.evomaster.clientJava.instrumentation.testability;

import java.util.Objects;

/**
 * Class used to replace calls to String methods.
 * For example, String#equals does return a boolean, which would
 * just give a flat plateau in the fitness landscape.
 */
public class StringTransformer extends BooleanMethodTransformer {

    public StringTransformer() {
        super(String.class);
    }

    //TODO could collect constants here for seeding

    @BooleanReplacement
    public static int equals(String caller, Object anObject) {
        Objects.requireNonNull(caller);

        if (anObject == null || !(anObject instanceof String)) {
            //worst possible case
            return BooleanReplacement.FALSE_MIN;
        }

        if (caller.equals(anObject)) {
            return BooleanReplacement.TRUE_MAX;
        }

        long distance = getLeftAlignmentDistance(caller, anObject.toString());
        /*
            must truncate if far too large distance, which cannot handle with ints.
            however, this should never happen unless very large strings,
            ie > 2^15 ~ 32k, ie 2^31 (max int) / 2^16 (max char)

            as not equal, returned value MUST be negative
        */
        return truncate(-distance);
    }

    @BooleanReplacement
    public static int equalsIgnoreCase(String caller, String anotherString) {
        Objects.requireNonNull(caller);

        if (anotherString == null) {
            return BooleanReplacement.FALSE_MIN;
        }

        if (caller.equalsIgnoreCase(anotherString)) {
            return BooleanReplacement.TRUE_MAX;
        }

        return equals(caller.toLowerCase(), anotherString.toLowerCase());
    }

    @BooleanReplacement
    public static int startsWith(String caller, String prefix, int toffset) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(prefix);

        int pl = prefix.length();

        /*
            The penalty when there is a mismatch of lengths/offset
            should be at least pl, as should be always worse than
            when doing "equals" comparisons.
            Furthermore, need to add extra penalty in case string is
            shorter than prefix
         */
        int penalty = pl;
        if(caller.length() < pl){
            penalty += (pl - caller.length());
        }

        if (toffset < 0) {
            long dist = (-toffset + penalty) * Character.MAX_VALUE;
            return truncate(-dist);
        }

        if (toffset > caller.length() - pl) {
            assert toffset >= 0;
            long dist = (toffset + penalty) * Character.MAX_VALUE;
            return truncate(-dist);
        }

        int len = Math.min(prefix.length(), caller.length());
        String sub = caller.substring(toffset, Math.min(toffset + len, caller.length()));

        return equals(sub, prefix);
    }

    @BooleanReplacement
    public static int startsWith(String caller, String prefix) {
        return startsWith(caller, prefix, 0);
    }

    @BooleanReplacement
    public static int endsWith(String caller, String suffix) {
        return startsWith(caller, suffix, caller.length() - suffix.length());
    }

    @BooleanReplacement
    public static int isEmpty(String caller) {
        Objects.requireNonNull(caller);

        int len = caller.length();
        if (len == 0) {
            return BooleanReplacement.TRUE_MAX;
        } else {
            return -len;
        }
    }

    @BooleanReplacement
    public static int contentEquals(String caller, CharSequence cs) {
        return equals(caller, cs.toString());
    }

    @BooleanReplacement
    public static int contentEquals(String caller, StringBuffer sb) {
        return equals(caller, sb.toString());
    }

    @BooleanReplacement
    public static int contains(String caller, CharSequence s) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(s);

        if (caller.contains(s)) {
            return BooleanReplacement.TRUE_MAX;
        }

        String k = s.toString();
        if (caller.length() <= k.length()) {
            return equals(caller, k);
        }

        assert caller.length() > k.length();
        int best = BooleanReplacement.FALSE_MIN;
        for (int i = 0; i < (caller.length() - k.length()) + 1; i++) {
            String sub = caller.substring(i, i + k.length());
            int h = equals(sub, k);
            if (h > best) {
                best = h;
            }
        }
        return best;
    }

    /*
        TODO:
        public boolean regionMatches(int toffset, String other, int ooffset, int len)
        public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len)
        public boolean matches(String regex)
     */

    private static int truncate(long value) {
        if (value > BooleanReplacement.TRUE_MAX) {
            return BooleanReplacement.TRUE_MAX;
        }
        if (value < BooleanReplacement.FALSE_MIN) {
            return BooleanReplacement.FALSE_MIN;
        }
        return (int) value;
    }

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
