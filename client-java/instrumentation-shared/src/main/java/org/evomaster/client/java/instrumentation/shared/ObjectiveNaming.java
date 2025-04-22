package org.evomaster.client.java.instrumentation.shared;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * Prefix identifier for successful, non-null checkcast instructions
     */
    public static final String CHECKCAST = "CheckCast";

    /**
     * Numeric comparison for non-integers, ie long, double and float
     */
    public static final String NUMERIC_COMPARISON = "NumericComparison";

    /*
        WARNING: originally where interning all strings, to save memory.
        but that looks like it was having quite a performance hit on LanguageTool.

        For the most used methods, added some memoization, as those methods look like
        among the most expensive/used during performance profiling.

        One problem though is due to multi-params for indexing... we could use unique global
        ids already at instrumentation time (to force a single lookup, instead of a chain
        of maps), but that would require a major refactoring.
     */

    private static final Map<String, String> cacheClass = new ConcurrentHashMap<>(10_000);

    /**
     *
     * @return prefixes of objectives
     */
    public static List<String> getAllObjectivePrefixes(){
        return Arrays.asList(BRANCH, LINE, CLASS, METHOD_REPLACEMENT, SUCCESS_CALL);
    }

    public static String classObjectiveName(String className){
        return cacheClass.computeIfAbsent(className, c -> CLASS + "_" + ClassName.get(c).getFullNameWithDots());
        //String name = CLASS + "_" + ClassName.get(className).getFullNameWithDots();
        //return name;//.intern();
    }

    public static String numericComparisonObjectiveName(String id, int res){
        String name = NUMERIC_COMPARISON + "_" + id + "_" + (res == 0 ? "EQ" : (res < 0 ? "LT" : "GT"));
        return name;//.intern();
    }


    private static final Map<String, Map<Integer, String>> lineCache = new ConcurrentHashMap<>(10_000);

    public static String lineObjectiveName(String className, int line){

        Map<Integer, String> map = lineCache.computeIfAbsent(className, c -> new ConcurrentHashMap<>(1000));
        return map.computeIfAbsent(line, l -> LINE + "_at_" + ClassName.get(className).getFullNameWithDots() + "_" + padNumber(line));
    }

    private static final Map<String, Map<Integer, Map<Integer, String>>> cacheCheckcast = new ConcurrentHashMap<>(10_000);

    public static String checkcastObjectiveName(String className, int line, int index){
        Map<Integer, Map<Integer, String>> m0 = cacheCheckcast.computeIfAbsent(className, c -> new ConcurrentHashMap<>(10_000));
        Map<Integer, String> m1 = m0.computeIfAbsent(line, l -> new ConcurrentHashMap<>(10));
        return m1.computeIfAbsent(index, i -> CHECKCAST + "_at_" + ClassName.get(className).getFullNameWithDots() +
                "_" + padNumber(line) + "_" + index);
    }

    private static final Map<String, Map<Integer, Map<Integer, String>>> cacheSuccessCall = new ConcurrentHashMap<>(10_000);

    public static String successCallObjectiveName(String className, int line, int index){
        Map<Integer, Map<Integer, String>> m0 = cacheSuccessCall.computeIfAbsent(className, c -> new ConcurrentHashMap<>(10_000));
        Map<Integer, String> m1 = m0.computeIfAbsent(line, l -> new ConcurrentHashMap<>(10));
        return m1.computeIfAbsent(index, i -> SUCCESS_CALL + "_at_" + ClassName.get(className).getFullNameWithDots() +
                "_" + padNumber(line) + "_" + index);
    }

    public static String methodReplacementObjectiveNameTemplate(String className, int line, int index){
        String name = METHOD_REPLACEMENT + "_at_" + ClassName.get(className).getFullNameWithDots() +
                "_" + padNumber(line) + "_" + index;
        return name;//.intern();
    }

    public static String methodReplacementObjectiveName(String template, boolean result, ReplacementType type){
        if(template==null || !template.startsWith(METHOD_REPLACEMENT)){
            throw new IllegalArgumentException("Invalid template for boolean method replacement: " + template);
        }
        String name = template + "_" + type.name() + "_" + result;
        return name;//.intern();
    }


    private static final Map<String, Map<Integer, Map<Integer, Map<Boolean, String>>>> branchCache = new ConcurrentHashMap<>(10_000);

    public static String branchObjectiveName(String className, int line, int branchId, boolean thenBranch, int opcode) {

        Map<Integer, Map<Integer, Map<Boolean, String>>> m0 = branchCache.computeIfAbsent(className, k -> new ConcurrentHashMap<>(10_000));
        Map<Integer, Map<Boolean, String>> m1 = m0.computeIfAbsent(line, k -> new ConcurrentHashMap<>(10));
        Map<Boolean, String> m2 = m1.computeIfAbsent(branchId, k -> new ConcurrentHashMap<>(2));

        return m2.computeIfAbsent(thenBranch, k -> {
            String name = BRANCH + "_at_" +
                    ClassName.get(className).getFullNameWithDots()
                    + "_at_line_" + padNumber(line) + "_position_" + branchId;
            if(thenBranch){
                name += TRUE_BRANCH;
            } else {
                name += FALSE_BRANCH;
            }
            name += "_" + opcode;
            return name;
        });

//        String name = BRANCH + "_at_" +
//                ClassName.get(className).getFullNameWithDots()
//                + "_at_line_" + padNumber(line) + "_position_" + branchId;
//        if(thenBranch){
//            name += TRUE_BRANCH;
//        } else {
//            name += FALSE_BRANCH;
//        }
//        return name;//.intern();
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
