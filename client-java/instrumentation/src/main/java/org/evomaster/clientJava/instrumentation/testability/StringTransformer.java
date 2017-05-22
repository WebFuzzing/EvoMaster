package org.evomaster.clientJava.instrumentation.testability;

/**
 * Class used to replace calls to String methods.
 * For example, String#equals does return a boolean, which would
 * just give a flat plateau in the fitness landscape.
 */
public class StringTransformer extends BooleanMethodTransformer {

    protected StringTransformer() {
        super(String.class);
    }

    @BooleanReplacement
    public static int equals(String caller, Object anObject) {
        if (caller == null) {
            throw new NullPointerException("");
        }

        if (anObject == null || !(anObject instanceof String)) {
            //worst possible case
            return BooleanReplacement.FALSE_MIN;
        }

        if (caller.equals(anObject)) {
            return BooleanReplacement.TRUE_MAX;
        }

        //TODO could collect constants here for seeding

        long distance = getLeftAlignmentDistance(caller, anObject.toString());
        if(-distance < BooleanReplacement.FALSE_MIN){
            /*
                far too large distance, which cannot handle with ints.
                however, this should never happen unless very large strings,
                ie > 2^15 ~ 32k, ie 2^31 (max int) / 2^16 (max char)
             */
            return BooleanReplacement.FALSE_MIN;
        }

        //as not equal, returned value MUST be negative
        return (int) -distance;
    }

    private static long getLeftAlignmentDistance(String a, String b){

        long diff = Math.abs(a.length() - b.length());
        long dist = diff * Character.MAX_VALUE;

        for(int i=0; i < Math.min(a.length(), b.length()); i++){
            dist += Math.abs(a.charAt(i) - b.charAt(i));
        }

        assert dist >= 0;
        return dist;
    }
}
