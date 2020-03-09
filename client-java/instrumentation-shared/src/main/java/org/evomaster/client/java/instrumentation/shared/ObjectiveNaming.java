package org.evomaster.client.java.instrumentation.shared;

import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.ClassName;

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
     * Prefix identifier for MethodReplacement objectives, where we want
     * to cover both possible outcomes, eg true and false
     */
    public static final String METHOD_REPLACEMENT = "MethodReplacement";


    /**
     * Prefix identifier for objectives related to calling methods without exceptions
     */
    public static final String SUCCESS_CALL = "Success_Call";

    /**
     * Numeric comparison for non-integers, ie long, double and float
     */
    public static final String NUMERIC_COMPARISON = "NumericComparison";


    public static String classObjectiveName(String className){
        String name = CLASS + "_" + ClassName.get(className).getFullNameWithDots();
        return name.intern();
    }

    public static String numericComparisonObjectiveName(String id, int res){
        String name = NUMERIC_COMPARISON + "_" + id + "_" + (res == 0 ? "EQ" : (res < 0 ? "LT" : "GT"));
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

    public static String methodReplacementObjectiveNameTemplate(String className, int line, int index){
        String name = METHOD_REPLACEMENT + "_at_" + ClassName.get(className).getFullNameWithDots() +
                "_" + padNumber(line) + "_" + index;
        return name.intern();
    }

    public static String methodReplacementObjectiveName(String template, boolean result, ReplacementType type){
        if(template==null || !template.startsWith(METHOD_REPLACEMENT)){
            throw new IllegalArgumentException("Invalid template for boolean method replacement: " + template);
        }
        String name = template + "_" + type.name() + "_" + result;
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
