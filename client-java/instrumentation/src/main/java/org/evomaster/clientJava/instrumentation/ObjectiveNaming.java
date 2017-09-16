package org.evomaster.clientJava.instrumentation;

public class ObjectiveNaming {

    /**
     * Prefix identifier for class coverage objectives.
     * A class is "covered" if at least one of its lines is executed.
     */
    public static final String CLASS = "Class";

    /**
     * Prefix identifier for line coverage objectives
     */
    public static final String LINE = "Line";

    /**
     * Prefix identifier for branch coverage objectives
     */
    public static final String BRANCH = "Branch";

    /**
     * Tag used in a branch id to specify it is for the "true"/then branch
     */
    public static final String TRUE_BRANCH = "_trueBranch";

    /**
     * Tag used in a branch id to specify it is for the "false"/else branch
     */
    public static final String FALSE_BRANCH = "_falseBranch";


    /**
     * Prefix identifier for objectives related to calling methods without exceptions
     */
    public static final String SUCCESS_CALL = "Success_Call";


    public static String classObjectiveName(String className){
        String name = CLASS + "_" + ClassName.get(className).getFullNameWithDots();
        return name.intern();
    }

    public static String lineObjectiveName(String className, int line){
        String name = LINE + "_at_" + ClassName.get(className).getFullNameWithDots() + "_" + padNumber(line);
        return name.intern();
    }

    public static String successCallObjectiveName(String className, int line, int index){
        String name = SUCCESS_CALL + "_at_" + ClassName.get(className).getFullNameWithDots() +
                "_" + padNumber(line) + "_" + index;
        return name.intern();
    }

    public static String branchObjectiveName(String className, int line, int branchId, boolean thenBranch) {

        String name = BRANCH + "_at_" +
                ClassName.get(className).getFullNameWithDots()
                + "_at_line_" + padNumber(line) + "_position_" + branchId;
        if(thenBranch){
            name += TRUE_BRANCH;
        } else {
            name += FALSE_BRANCH;
        }
        return name.intern();
    }

    private static String padNumber(int val){
        if(val < 0){
            throw new IllegalArgumentException("Negative number to pad");
        }
        if(val < 10){
            return "0000" + val;
        }
        if(val < 100){
            return "000" + val;
        }
        if(val < 1_000){
            return "00" + val;
        }
        if(val < 10_000){
            return "0" + val;
        } else {
            return ""+val;
        }
    }
}
