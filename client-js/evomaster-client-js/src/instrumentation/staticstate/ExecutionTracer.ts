/**
 * Methods of this class will be injected in the SUT to
 * keep track of what the tests do execute/cover.
 */
import TargetInfo from "../TargetInfo";
import Action from "../Action";
import ObjectiveNaming from "../ObjectiveNaming";
import AdditionalInfo from "../AdditionalInfo";

export default class ExecutionTracer {

    /*
        Careful if you change the signature of any of the
        methods in this class, as they are injected in the
        bytecode instrumentation.
        Fortunately, unit tests should quickly find such
        type of issues.
     */

    /**
     * Key -> the unique descriptive id of the coverage objective
     */
    private static readonly objectiveCoverage: Map<string, TargetInfo> = new Map<string, TargetInfo>();

    /**
     * A test case can be composed by 1 or more actions, eg HTTP calls.
     * When we get the best distance for a testing target, we might
     * also want to know which action in the test led to it.
     */
    private static actionIndex: number = 0;

    /**
     * A set of possible values used in the tests, needed for some kinds
     * of taint analyses
     */
    private static inputVariables: Set<string> = new Set();

    /**
     * Besides code coverage, there might be other events that we want to
     * keep track during test execution.
     * We keep track of it separately for each action
     */
    private static readonly additionalInfoList: Array<AdditionalInfo> = [];


    static reset() {
        ExecutionTracer.objectiveCoverage.clear();
        ExecutionTracer.actionIndex = 0;
        ExecutionTracer.additionalInfoList.length = 0;
        ExecutionTracer.additionalInfoList.push(new AdditionalInfo());
        ExecutionTracer.inputVariables = new Set();
    }


    static setAction(action: Action) {
        if (action.getIndex() != ExecutionTracer.actionIndex) {
            ExecutionTracer.actionIndex = action.getIndex();
            ExecutionTracer.additionalInfoList.push(new AdditionalInfo());
        }

        if (action.getInputVariables() && action.getInputVariables().size > 0) {
            ExecutionTracer.inputVariables = action.getInputVariables();
        }
    }

    /**
     * Check if the given input represented a tainted value from the test cases.
     * This could be based on static info of the input (eg, according to a precise
     * name convention given by TaintInputName), or dynamic info given directly by
     * the test itself (eg, the test at action can register a list of values to check
     * for)
     */
    // public static boolean isTaintInput(String input){
    //     return TaintInputName.isTaintInput(input) || inputVariables.contains(input);
    // }


    // public static TaintType getTaintType(String input){
    //
    //     if(input == null){
    //         return TaintType.NONE;
    //     }
    //
    //     if(isTaintInput(input)){
    //         return TaintType.FULL_MATCH;
    //     }
    //
    //     if(TaintInputName.includesTaintInput(input)
    //         || inputVariables.stream().anyMatch(v -> input.contains(v))){
    //         return TaintType.PARTIAL_MATCH;
    //     }
    //
    //     return TaintType.NONE;
    // }


    static exposeAdditionalInfoList(): Array<AdditionalInfo> {
        return ExecutionTracer.additionalInfoList;
    }

    // public static void addQueryParameter(String param){
    //     additionalInfoList.get(actionIndex).addQueryParameter(param);
    // }
    //
    // public static void addHeader(String header){
    //     additionalInfoList.get(actionIndex).addHeader(header);
    // }
    //
    // public static void addStringSpecialization(String taintInputName, StringSpecializationInfo info){
    //     additionalInfoList.get(actionIndex).addSpecialization(taintInputName, info);
    // }


    public static markLastExecutedStatement(lastLine: string) {

        /*
            There is a possible issue here: when there is an exception, there
            is no pop of the stmt. So, the "call-stack" until the exception will still
            be saved in this stack, even if computation continues (eg after a try/catch).
            This is possibly a memory leak
         */

        ExecutionTracer.additionalInfoList[ExecutionTracer.actionIndex]
            .pushLastExecutedStatement(lastLine);
    }


    public static completedLastExecutedStatement(lastLine: string) {
        const stmt = ExecutionTracer.additionalInfoList[ExecutionTracer.actionIndex].popLastExecutedStatement();
        if(stmt !== lastLine){
            throw Error(`Expected to pop ${lastLine} instead of ${stmt}`);
        }
    }

    public static getInternalReferenceToObjectiveCoverage(): Map<String, TargetInfo> {
        return ExecutionTracer.objectiveCoverage;
    }

    /**
     * @return the number of objectives that have been encountered
     * during the test execution
     */
    public static getNumberOfObjectives(): number {
        return ExecutionTracer.objectiveCoverage.size;
    }

// public static int getNumberOfObjectives(String prefix) {
//     return (int) objectiveCoverage
//         .entrySet().stream()
//         .filter(e -> prefix == null || e.getKey().startsWith(prefix))
//         .count();
// }

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
// public static int getNumberOfNonCoveredObjectives(String prefix) {
//
//     return getNonCoveredObjectives(prefix).size();
// }

// public static Set<String> getNonCoveredObjectives(String prefix) {
//
//     return objectiveCoverage
//         .entrySet().stream()
//         .filter(e -> prefix == null || e.getKey().startsWith(prefix))
//         .filter(e -> e.getValue().value < 1)
//         .map(e -> e.getKey())
//         .collect(Collectors.toSet());
// }

    public static getValue(id: string): number {
        return ExecutionTracer.objectiveCoverage.get(id).value;
    }

    private static updateObjective(id: string, value: number) {
        if (value < 0 || value > 1) {
            throw new Error("Invalid value " + value + " out of range [0,1]");
        }

        /*
            In the same execution, a target could be reached several times,
            so we should keep track of the best value found so far
         */
        if (ExecutionTracer.objectiveCoverage.has(id)) {
            let previous = ExecutionTracer.objectiveCoverage.get(id).value;
            if (value > previous) {
                ExecutionTracer.objectiveCoverage.set(id, new TargetInfo(null, id, value, ExecutionTracer.actionIndex));
            }
        } else {
            ExecutionTracer.objectiveCoverage.set(id, new TargetInfo(null, id, value, ExecutionTracer.actionIndex));
        }

        //ObjectiveRecorder.update(id, value);
    }

// public static executedReplacedMethod(idTemplate: string, ReplacementType type, Truthness t){
//
//     String idTrue = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, true, type);
//     String idFalse = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, false, type);
//
//     updateObjective(idTrue, t.getOfTrue());
//     updateObjective(idFalse, t.getOfFalse());
// }


    /**
     *
     * WARNING: here we do differently from Java, as we can not rely on reflection
     * to get unique id for methods.
     *
     * We rather do "statement" coverage, and have a further id for it.
     */
    public static enteringStatement(fileName: string, line: number, statementId: number) {

        const lineId = ObjectiveNaming.lineObjectiveName(fileName, line);
        const fileId = ObjectiveNaming.classObjectiveName(fileName);
        ExecutionTracer.updateObjective(lineId, 1);
        ExecutionTracer.updateObjective(fileId, 1);

        //TODO statement target

        const lastLine = fileName + "_" + line+"_" + statementId;

        ExecutionTracer.markLastExecutedStatement(lastLine);
    }

    public static completedStatement(fileName: string, line: number, statementId: number){

        //TODO statement target

        const lastLine = fileName + "_" + line+"_" + statementId;

        ExecutionTracer.completedLastExecutedStatement(lastLine);
    }


    /**
     *  Report on whether method calls have been successfully completed.
     *  Failures can happen due to thrown exceptions.
     *
     * @param className
     * @param line
     * @param index    as there can be many method calls on same line, need to differentiate them
     * @param completed whether the method call was successfully completed.
     */
    public static executingMethod(fileName: string, line: number, index: number, completed: boolean) {
        const id = ObjectiveNaming.successCallObjectiveName(fileName, line, index);
        if (completed) {
            ExecutionTracer.updateObjective(id, 1);
        } else {
            ExecutionTracer.updateObjective(id, 0.5);
        }
    }


//---- branch-jump methods --------------------------
//
// private static void updateBranch(String className, int line, int branchId, Truthness t) {
//
//     /*
//         Note: when we have
//         if(x > 0){}
//
//         the "jump" to "else" branch is done if that is false.
//         So, the actual evaluated condition is the negation, ie
//         x <= 0
//      */
//
//     String forThen = ObjectiveNaming.branchObjectiveName(className, line, branchId, true);
//     String forElse = ObjectiveNaming.branchObjectiveName(className, line, branchId, false);
//
//     updateObjective(forElse, t.getOfTrue());
//     updateObjective(forThen, t.getOfFalse());
// }


// public static void executingBranchJump(
//     int value, int opcode, String className, int line, int branchId) {
//
//     Truthness t = HeuristicsForJumps.getForSingleValueJump(value, opcode);
//
//     updateBranch(className, line, branchId, t);
// }


// public static void executingBranchJump(
//     int firstValue, int secondValue, int opcode, String className, int line, int branchId) {
//
//     Truthness t = HeuristicsForJumps.getForValueComparison(firstValue, secondValue, opcode);
//
//     updateBranch(className, line, branchId, t);
// }


// public static void executingBranchJump(
//     Object first, Object second, int opcode, String className, int line, int branchId) {
//
//     Truthness t = HeuristicsForJumps.getForObjectComparison(first, second, opcode);
//
//     updateBranch(className, line, branchId, t);
// }


// public static void executingBranchJump(
//     Object obj, int opcode, String className, int line, int branchId) {
//
//     Truthness t = HeuristicsForJumps.getForNullComparison(obj, opcode);
//
//     updateBranch(className, line, branchId, t);
// }

}


ExecutionTracer.reset();