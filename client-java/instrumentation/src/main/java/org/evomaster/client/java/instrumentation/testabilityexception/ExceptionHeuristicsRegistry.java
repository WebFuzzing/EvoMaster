package org.evomaster.client.java.instrumentation.testabilityexception;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class ExceptionHeuristicsRegistry {

    /*
        TODO: in long run, should have some reflection like we do with BooleanReplacement
     */

    public static boolean shouldHandle(String owner, String name, String desc) {

        return isLocalDateParseCharSequence(owner, name, desc)
                || isIntegerParseIntString(owner, name, desc);
    }

    public static int numberOfInputs(String owner, String name, String desc) {
        //TODO for now, we only deal with static methods with single object input
        return 1;
    }

    public static double computeHeuristics(Object input, String owner, String name, String desc) {

        if(isLocalDateParseCharSequence(owner, name, desc)){
            return LocalDateExceptionHeuristics.parse((String) input);

        } else if(isIntegerParseIntString(owner, name, desc)){
            return IntegerExceptionHeuristics.parseInt((String) input);

        } else {
            throw new IllegalArgumentException("Cannot handle: " + owner + name + desc);
        }
    }


    private static boolean isLocalDateParseCharSequence(String owner, String name, String desc) {
        return owner.equals("java/time/LocalDate") && name.equals("parse")
                && desc.equals("(Ljava/lang/CharSequence;)Ljava/time/LocalDate;");
    }

    private static boolean isIntegerParseIntString(String owner, String name, String desc) {
        return owner.equals("java/lang/Integer") && name.equals("parseInt")
                && desc.equals("(Ljava/lang/String;)I");
    }
}
