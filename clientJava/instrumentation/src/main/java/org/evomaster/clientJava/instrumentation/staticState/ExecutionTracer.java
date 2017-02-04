package org.evomaster.clientJava.instrumentation.staticState;

import org.evomaster.clientJava.instrumentation.ClassName;
import org.evomaster.clientJava.instrumentation.heuristic.HeuristicsForJumps;
import org.evomaster.clientJava.instrumentation.heuristic.Truthness;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Methods of this class will be injected in the SUT to
 * keep track of what the tests do execute/cover.
 */
public class ExecutionTracer {

    /*
        Careful if you change the signature of any of the
        methods in this class, as they are injected in the
        bytecode instrumentation.
        Fortunately, unit tests should quickly find such
        type of issues.
     */

    /**
     * Prefix identifier for line coverage objectives
     */
    public static final String LINE = "Line";

    /**
     * Prefix identifier for branch coverage objectives
     */
    public static final String BRANCH = "Branch";


    /**
     * Key -> the unique id of the coverage objective
     * <br>
     * Value -> heuristic [0,1], where 1 means covered
     */
    private static final Map<String, Double> objectiveCoverage =
            new ConcurrentHashMap<>(65536);


    public static void reset() {
        objectiveCoverage.clear();
    }


    public static Map<String, Double> getInternalReferenceToObjectiveCoverage() {
        return objectiveCoverage;
    }

    /**
     * @return the number of objectives that have been encountered
     * during the test execution
     */
    public static int getNumberOfObjectives() {
        return objectiveCoverage.size();
    }

    /**
     * Note: only the objectives encountered so far can have
     * been recorded. So, this is a relative value, not based
     * on the code of the whole SUT (just the parts executed so far).
     * Therefore, it is quite useless for binary values (ie 0 or 1),
     * like current implementation of basic line coverage.
     *
     * @param prefix used for string matching of which objectives types
     *               to consider, eg only lines or only branches.
     *               Use "" or {@code null} to pick up everything
     * @return
     */
    public static int getNumberOfNonCoveredObjectives(String prefix) {

        return (int) objectiveCoverage
                .entrySet().stream()
                .filter(e -> prefix == null || e.getKey().startsWith(prefix))
                .filter(e -> e.getValue() < 1)
                .count();
    }


    private static void updateObjective(String id, double value) {
        if (value < 0d || value > 1d) {
            throw new IllegalArgumentException("Invalid value " + value + " out of range [0,1]");
        }

        /*
            In the same execution, a target could be reached several times,
            so we should keep track of the best value found so far
         */
        if(objectiveCoverage.containsKey(id)){
            double previous = objectiveCoverage.get(id);
            objectiveCoverage.put(id, Math.max(value, previous));
        } else {
            objectiveCoverage.put(id, value);
        }

        ObjectiveRecorder.update(id, value);
    }


    public static final String EXECUTED_LINE_METHOD_NAME = "executedLine";
    public static final String EXECUTED_LINE_DESCRIPTOR = "(Ljava/lang/String;I)V";

    /**
     * Report on the fact that a given line has been executed.
     */
    public static void executedLine(String className, int line) {

        String id = LINE + "_" + line + "_at_" + ClassName.get(className).getFullNameWithDots();
        updateObjective(id, 1d);
    }


    //---- branch-jump methods --------------------------

    private static void updateBranch(String id, Truthness t){
        updateObjective(id+"_trueBranch", t.getOfTrue());
        updateObjective(id+"_falseBranch", t.getOfFalse());
    }

    private static String getUniqueBranchId(String className, int line, int branchId) {

        return BRANCH + "_at_line_"+line+"_position_"+branchId+"_at_" +
                ClassName.get(className).getFullNameWithDots();
    }


    public static final String EXECUTING_BRANCH_JUMP_METHOD_NAME = "executingBranchJump";


    public static final String JUMP_DESC_1_VALUE = "(IILjava/lang/String;II)V";
    public static void executingBranchJump(
            int value, int opcode, String className, int line, int branchId) {

        String id = getUniqueBranchId(className, line, branchId);
        Truthness t = HeuristicsForJumps.getForSingleValueJump(value, opcode);

        updateBranch(id, t);
    }


    public static final String JUMP_DESC_2_VALUES = "(IIILjava/lang/String;II)V";
    public static void executingBranchJump(
            int firstValue, int secondValue, int opcode, String className, int line, int branchId) {

        String id = getUniqueBranchId(className, line, branchId);
        //TODO: make sure the order is correct, as possible issue with JVM stack
        Truthness t = HeuristicsForJumps.getForValueComparison(firstValue, secondValue, opcode);

        updateBranch(id, t);
    }

    public static final String JUMP_DESC_OBJECTS =
            "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/String;II)V";
    public static void executingBranchJump(
            Object first, Object second, int opcode, String className, int line, int branchId) {

        String id = getUniqueBranchId(className, line, branchId);
        Truthness t = HeuristicsForJumps.getForObjectComparison(first, second, opcode);

        updateBranch(id, t);
    }


    public static final String JUMP_DESC_NULL =
            "(Ljava/lang/Object;ILjava/lang/String;II)V";
    public static void executingBranchJump(
            Object obj, int opcode, String className, int line, int branchId) {

        String id = getUniqueBranchId(className, line, branchId);
        Truthness t = HeuristicsForJumps.getForNullComparison(obj, opcode);

        updateBranch(id, t);
    }
}
