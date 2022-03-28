/**
 * Methods of this class will be injected in the SUT to
 * keep track of what the tests do execute/cover.
 */
import TargetInfo from "../TargetInfo";
import Action from "../Action";
import ObjectiveNaming from "../shared/ObjectiveNaming";
import AdditionalInfo from "../AdditionalInfo";
import ObjectiveRecorder from "./ObjectiveRecorder";
import Truthness from "../heuristic/Truthness";
import HeuristicsForBooleans from "../heuristic/HeuristicsForBooleans";
import {ReplacementType} from "../methodreplacement/ReplacementType";
import {TaintType} from "../shared/TaintType";
import {TaintInputName} from "../shared/TaintInputName";
import {StringSpecializationInfo} from "../shared/StringSpecializationInfo";
import {StringSpecialization} from "../shared/StringSpecialization";

export default class ExecutionTracer {

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
    static isTaintInput(input: string): boolean {
        return TaintInputName.isTaintInput(input) || ExecutionTracer.inputVariables.has(input);
    }

    static handleTaintForStringEquals(left: string, right: string, ignoreCase: boolean) {

        /*
            Note: cannot use !left, as that would be true empty strings ""... isn't JS wonderful???
         */
        if (left == null || right == null || left == undefined || right == undefined) {
            //nothing to do?
            return;
        }

        const taintedLeft = ExecutionTracer.isTaintInput(left);
        const taintedRight = ExecutionTracer.isTaintInput(right);

        if (taintedLeft && taintedRight) {
            if (ignoreCase ? left.toLowerCase() === right.toLowerCase() : left === right) {
                //tainted, but compared to itself. so shouldn't matter
                return;
            }

            /*
                We consider binding only for base versions of taint, ie we ignore
                the special strings provided by the Core, as it would lead to nasty
                side-effects
             */
            if (!TaintInputName.isTaintInput(left) || !TaintInputName.isTaintInput(right)) {
                return;
            }

            //TODO could have EQUAL_IGNORE_CASE
            const id = left + "___" + right;
            ExecutionTracer.addStringSpecialization(left, new StringSpecializationInfo(StringSpecialization.EQUAL, id));
            ExecutionTracer.addStringSpecialization(right, new StringSpecializationInfo(StringSpecialization.EQUAL, id));
            return;
        }

        const type = ignoreCase ? StringSpecialization.CONSTANT_IGNORE_CASE
            : StringSpecialization.CONSTANT;

        if (taintedLeft || taintedRight) {
            if (taintedLeft) {
                ExecutionTracer.addStringSpecialization(left, new StringSpecializationInfo(type, right));
            } else {
                ExecutionTracer.addStringSpecialization(right, new StringSpecializationInfo(type, left));
            }
        }
    }


    static getTaintType(input: string): TaintType {

        if (input == null) {
            return TaintType.NONE;
        }

        if (ExecutionTracer.isTaintInput(input)) {
            return TaintType.FULL_MATCH;
        }

        if (TaintInputName.includesTaintInput(input)
            || Array.from(ExecutionTracer.inputVariables).some(v => input.includes(v))) {
            return TaintType.PARTIAL_MATCH;
        }

        return TaintType.NONE;
    }


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
    static  addStringSpecialization(taintInputName: string, info: StringSpecializationInfo){
        ExecutionTracer.additionalInfoList[ExecutionTracer.actionIndex].addSpecialization(taintInputName, info);
    }


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
        if (stmt !== lastLine) {
            /*
                actually we cannot have such check. We might end in such situation:

                X calls F in non-instrumented framework, which then call Y (both X and Y being of SUT).
                If Y crashes with a catch in F, then X will wrongly pop for Y.

                TODO could have such check with a parameter, to have only in the tests
             */
            //throw Error(`Expected to pop ${lastLine} instead of ${stmt}`);
        }
    }

    public static getInternalReferenceToObjectiveCoverage(): Map<String, TargetInfo> {
        return ExecutionTracer.objectiveCoverage;
    }

    /**
     * @return the number of objectives that have been encountered
     * during the test execution
     */
    public static getNumberOfObjectives(prefix?: string): number {

        if (!prefix) {
            return ExecutionTracer.objectiveCoverage.size;
        }

        return Array.from(ExecutionTracer.objectiveCoverage.keys())
            .filter(e => e.startsWith(prefix))
            .length;
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
    public static getNumberOfNonCoveredObjectives(prefix: string): number {

        return ExecutionTracer.getNonCoveredObjectives(prefix).size;
    }

    public static getNonCoveredObjectives(prefix: string): Set<string> {

        return new Set(Array.from(ExecutionTracer.objectiveCoverage.entries())
            .filter(e => !prefix || e[0].startsWith(prefix))
            .filter(e => e[1].value < 1)
            .map(e => e[0])
        );
    }

    public static getValue(id: string): number {
        return ExecutionTracer.objectiveCoverage.get(id).value;
    }

    /**
     * @param id specifies the target id
     * @return whether the target is reached
     *
     * note that it is only useful for testing
     */
    public static isTargetReached(id: string): boolean {
        return ExecutionTracer.objectiveCoverage.has(id);
    }

    public static updateObjective(id: string, value: number) {
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

        ObjectiveRecorder.update(id, value);
    }

    public static executedReplacedMethod(idTemplate: string, type: ReplacementType, t: Truthness) {

        const idTrue = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, true, type);
        const idFalse = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, false, type);

        ExecutionTracer.updateObjective(idTrue, t.getOfTrue());
        ExecutionTracer.updateObjective(idFalse, t.getOfFalse());
    }


    /**
     *
     * WARNING: here we do differently from Java, as we can not rely on reflection
     * to get unique id for methods.
     *
     * We rather do "statement" coverage, and have a further id for it.
     */
    public static enteringStatement(fileName: string, line: number, statementId: number) {

        const lineId = ObjectiveNaming.lineObjectiveName(fileName, line);
        const fileId = ObjectiveNaming.fileObjectiveName(fileName);
        const stmtId = ObjectiveNaming.statementObjectiveName(fileName, line, statementId);
        ExecutionTracer.updateObjective(lineId, 1);
        ExecutionTracer.updateObjective(fileId, 1);
        ExecutionTracer.updateObjective(stmtId, 0.5);


        const lastLine = fileName + "_" + line + "_" + statementId;

        ExecutionTracer.markLastExecutedStatement(lastLine);
    }

    public static completedStatement(fileName: string, line: number, statementId: number) {

        const stmtId = ObjectiveNaming.statementObjectiveName(fileName, line, statementId);
        ExecutionTracer.updateObjective(stmtId, 1);

        const lastLine = fileName + "_" + line + "_" + statementId;

        ExecutionTracer.completedLastExecutedStatement(lastLine);

        HeuristicsForBooleans.clearLastEvaluation();
    }


    /**
     *  Report on whether method calls have been successfully completed.
     *  Failures can happen due to thrown exceptions.
     *
     * @param fileName
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


    public static updateBranch(fileName: string, line: number, branchId: number, t: Truthness) {

        const forThen = ObjectiveNaming.branchObjectiveName(fileName, line, branchId, true);
        const forElse = ObjectiveNaming.branchObjectiveName(fileName, line, branchId, false);

        ExecutionTracer.updateObjective(forThen, t.getOfTrue());
        ExecutionTracer.updateObjective(forElse, t.getOfFalse());
    }
}


ExecutionTracer.reset();