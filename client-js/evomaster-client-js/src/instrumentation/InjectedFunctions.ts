/*
    This is a bit different from Controller in Java.
    In Java, each class is uniquely identified by its package, which is
    what used in the bytecode, where "import" in the source code is just
    syntactic sugar.
    But here in JavaScript we need to import a module in the instrumented files,
    and the import itself must be added as part of the instrumentation.
    To avoid mess in scope resolutions, all EM functions that can be called/injected
    in the SUT will be defined here
 */


import ExecutionTracer from "./staticstate/ExecutionTracer";
import HeuristicsForBooleans from "./heuristic/HeuristicsForBooleans";
import ObjectiveRecorder from "./staticstate/ObjectiveRecorder";

export default class InjectedFunctions {

    public static registerTargets(idArray: Array<string>){
        for(let id of idArray){
            ObjectiveRecorder.registerTarget(id);
        }
    }

    public static enteringStatement(fileName: string, line: number, statementId: number) {
        ExecutionTracer.enteringStatement(fileName, line, statementId);
    }

    public static completedStatement(fileName: string, line: number, statementId: number) {
        ExecutionTracer.completedStatement(fileName, line, statementId);
    }


    public static completingStatement(value: any, fileName: string, line: number, statementId: number): any {
        ExecutionTracer.completedStatement(fileName, line, statementId);
        return value;
    }

    /**
     *  Used for statements like:
     *  - return (with no data)
     *  - continue
     *  - break
     *  - throw
     */
    public static markStatementForCompletion(fileName: string, line: number, statementId: number) {
        InjectedFunctions.enteringStatement(fileName, line, statementId);
        InjectedFunctions.completedStatement(fileName, line, statementId);
    }


    public static cmp(left: any, op: string, right: any, fileName: string, line: number, branchId: number): any {
        return HeuristicsForBooleans.evaluate(left,op,right,fileName,line,branchId);
    }


    public static or(left: () => any, right: () => any, isRightPure: boolean, fileName: string, line: number, branchId: number): any {
        return HeuristicsForBooleans.evaluateOr(left, right, isRightPure, fileName, line, branchId);
    }

    public static and(left: () => any, right: () => any, isRightPure: boolean, fileName: string, line: number, branchId: number): any {
        return HeuristicsForBooleans.evaluateAnd(left, right, isRightPure, fileName, line, branchId);
    }

    public static not(value: any) : any{
        return HeuristicsForBooleans.handleNot(value);
    }

}